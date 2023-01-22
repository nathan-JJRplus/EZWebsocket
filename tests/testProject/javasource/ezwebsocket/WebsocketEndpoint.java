package ezwebsocket;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.RemoteEndpoint.Async;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.ISession;

import com.mendix.thirdparty.org.json.JSONObject;

public class WebsocketEndpoint extends Endpoint {
  public ILogNode LOG;
  private long sessionTimeout;
  private String onCloseMicroflow;
  private String onCloseMicroflowParameterKey;

  public WebsocketEndpoint(String websocketIdentifier, long sessionTimeout) {
    super();
    this.sessionTimeout = sessionTimeout;
    this.LOG = Core.getLogger(websocketIdentifier);
  }

  public WebsocketEndpoint(String websocketIdentifier, long sessionTimeout, String onCloseMicroflow,
      String onCloseMicroflowParameterKey) {
    super();
    this.sessionTimeout = sessionTimeout;
    this.LOG = Core.getLogger(websocketIdentifier);
    this.onCloseMicroflow = onCloseMicroflow;
    this.onCloseMicroflowParameterKey = onCloseMicroflowParameterKey;
  }

  void notify(String objectId, String action) {
    // Retrieve all subscriptions by objectId
    Set<Async> remotes = this.getSubscriptions(objectId);
    // Send actiontrigger to all sessions
    for (Async remote : remotes) {
      try {
        remote.sendText(action);
      } catch (RuntimeException re) {
        if (LOG.isDebugEnabled())
          LOG.debug("RuntimeException while sending: " + re.getMessage());
      }
    }
  }

  // Map tracking all the sessions and the object they are subscribed to
  private Map<Session, Map<String, String>> subscriptions = new HashMap<Session, Map<String, String>>();

  private void addSubscription(Session session, String jsonData) {
    try {
      Map<String, String> parameters = parseJsonData(jsonData);
      validateSession(parameters.get("csrfToken"));
      if (LOG.isTraceEnabled())
        LOG.trace("Adding subscription: " + session.getId() + " for objectId: " + parameters.get("objectId"));
      subscriptions.put(session, parameters);
    } catch (RuntimeException re) {
      throw new RuntimeException("Connection refused: " + re.getMessage());
    }

  }

  private void removeSubscription(Session session) {
    if (LOG.isTraceEnabled())
      LOG.trace("Removing subscription: " + session.getId());
    // If onCloseMicroflow is configured, execute it
    if (!onCloseMicroflow.isEmpty()) {
      if (LOG.isTraceEnabled())
        LOG.trace("Executing onCloseMicroflow " + onCloseMicroflow);
      String onCloseMicroflowParameterValue = subscriptions.get(session).get("onCloseMicroflowParameterValue");
      Core.microflowCall(onCloseMicroflow).withParam(onCloseMicroflowParameterKey, onCloseMicroflowParameterValue)
          .execute(Core.createSystemContext());
    }
    subscriptions.remove(session);
  }

  // Retrieve all sessions subscribed to given object
  private Set<Async> getSubscriptions(String objectId) {
    Set<Async> remoteSet = new HashSet<Async>();
    subscriptions.forEach((key, value) -> {
      if (value.get("objectId").equals(objectId)) {
        remoteSet.add(key.getAsyncRemote());
      }
    });
    return remoteSet;
  }

  @Override
  public void onOpen(Session session, EndpointConfig config) {
    session.setMaxIdleTimeout(sessionTimeout * 1000);
    session.addMessageHandler(new MessageHandler.Whole<String>() {
      @Override
      public void onMessage(String jsonData) {
        // The only message the server expects is the message sent from the widget on
        // connection open
        try {
          addSubscription(session, jsonData);
        } catch (RuntimeException e) {
          try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, e.getMessage()));
          } catch (IOException ioe) {
            LOG.error(ioe);
          }
        }
      }
    });
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    if (subscriptions.containsKey(session))
      removeSubscription(session);
    if (LOG.isDebugEnabled())
      LOG.debug("Received onClose call with reason: " + closeReason.getCloseCode() + ", " + closeReason.getReasonPhrase());
  }

  private Map<String, String> parseJsonData(String jsonData) {
    try {
      JSONObject json = new JSONObject(jsonData);
      String objectId = json.getString("objectId");
      String csrfToken = json.getString("csrfToken");
      String onCloseMicroflowParameterValue = json.optString("onCloseMicroflowParameterValue");
      Map<String, String> parameters = new HashMap<String, String>();
      parameters.put("objectId", objectId);
      parameters.put("csrfToken", csrfToken);
      parameters.put("onCloseMicroflowParameterValue", onCloseMicroflowParameterValue);
      return parameters;
    } catch (RuntimeException re) {
      throw new RuntimeException("Invalid payload: " + re.getMessage());
    }

  }

  private boolean validateSession(String csrfToken) {
    Collection<? extends ISession> activeSessions = Core.getActiveSessions();
    while (activeSessions.iterator().hasNext()) {
      ISession session = activeSessions.iterator().next();
      if (session.getCsrfToken().equals(csrfToken))
        return true;
    }
    throw new RuntimeException("Invalid csrfToken");
  }

}