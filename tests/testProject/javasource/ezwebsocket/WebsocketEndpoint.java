package ezwebsocket;

import javax.websocket.CloseReason;
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

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

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
      } catch (RuntimeException e) {
        if (LOG.isDebugEnabled())
          LOG.debug("RuntimeException while sending: " + e.getMessage());
      }
    }
  }

  // Map tracking all the sessions and the object they are subscribed to
  private Map<Session, Map<String, String>> subscriptions = new HashMap<Session, Map<String, String>>();

  private void addSubscription(Session session, String jsonData) {
    Map<String, String> parameters = parseJsonData(jsonData);
    if (LOG.isTraceEnabled())
      LOG.trace("Adding subscription: " + session.getId() + " for objectId: " + parameters.get("objectId"));
    subscriptions.put(session, parameters);
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
        // which is the objectId to subscribe the session to
        if (jsonData.isEmpty()) {
          try {
            session.close();
          } catch (IOException e) {
            if (LOG.isDebugEnabled())
              LOG.debug(e.toString());
          }
        } else {
          addSubscription(session, jsonData);
        }
      }
    });
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    removeSubscription(session);
    if (LOG.isDebugEnabled())
      LOG.debug("Received onClose call with reason: " + closeReason);
  }

  private Map<String, String> parseJsonData(String jsonData) {
    JSONObject json = new JSONObject(jsonData);
    String objectId = json.getString("objectId");
    String onCloseMicroflowParameterValue = json.optString("onCloseMicroflowParameterValue");
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("objectId", objectId);
    parameters.put("onCloseMicroflowParameterValue", onCloseMicroflowParameterValue);
    return parameters;
  }

}