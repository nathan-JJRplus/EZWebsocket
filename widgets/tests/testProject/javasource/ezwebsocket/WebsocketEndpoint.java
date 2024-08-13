package ezwebsocket;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.Session;

import java.io.IOException;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.thirdparty.org.json.JSONException;
import com.mendix.thirdparty.org.json.JSONObject;

public class WebsocketEndpoint extends Endpoint {
  public ILogNode LOG;
  private long sessionTimeout;
  private boolean onCloseMicroflowEnabled = false;
  private String onCloseMicroflow;
  private String onCloseMicroflowParameterKey;

  private SessionManager sessionManager;

  public WebsocketEndpoint(String websocketIdentifier, long sessionTimeout) {
    super();
    this.sessionTimeout = sessionTimeout;
    this.LOG = Core.getLogger(websocketIdentifier);
    this.sessionManager = new SessionManager(LOG);
  }

  public WebsocketEndpoint(String websocketIdentifier, long sessionTimeout,
      String onCloseMicroflow,
      String onCloseMicroflowParameterKey) {
    this(websocketIdentifier, sessionTimeout);
    this.onCloseMicroflowEnabled = true;
    this.onCloseMicroflow = onCloseMicroflow;
    this.onCloseMicroflowParameterKey = onCloseMicroflowParameterKey;
  }

  @Override
  public void onOpen(Session session, EndpointConfig config) {
    LOG.trace(config.getUserProperties().entrySet().toString());
    session.setMaxIdleTimeout(sessionTimeout * 1000);
    session.addMessageHandler(new MessageHandler.Whole<String>() {
      @Override
      public void onMessage(String data) {
        try {
          addSubscription(session, data);
        } catch (RuntimeException e) {
          try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, e.getMessage()));
          } catch (IOException ioe) {
            LOG.error(ioe);
          }
        }
      }
    });
    session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
      @Override
      public void onMessage(PongMessage pongMessage) {
        try {1
          if (LOG.isTraceEnabled()) {
            LOG.trace("Received pong for session " + session.getId());
          }
          sessionManager.handlePong(session);
        } catch (RuntimeException e) {
          try {
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.getMessage()));
          } catch (IOException ioe) {
            LOG.error(ioe);
          }
        }
      }
    });
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    removeSubscription(session, closeReason);
  }

  void notify(String objectId, String action, String message) {
    // Construct message
    JSONObject payloadJSON = new JSONObject();
    payloadJSON.put("action", action);
    payloadJSON.put("message", message);
    String payload = payloadJSON.toString();

    sessionManager.notify(objectId, payload);
  }

  private void addSubscription(Session session, String jsonData) {
    try {
      JSONObject json = new JSONObject(jsonData);
      String objectId = json.getString("objectId");
      String csrfToken = json.getString("csrfToken");
      String onCloseMicroflowParameterValue = json.optString("onCloseMicroflowParameterValue");

      sessionManager.registerSubscription(session, csrfToken, objectId, onCloseMicroflowParameterValue);

    } catch (JSONException je) {
      LOG.error("Error occured during parsing JSONdata: ", je);
    } catch (RuntimeException re) {
      throw new RuntimeException("Connection refused: " + re.getMessage());
    }
  }

  private void removeSubscription(Session session, CloseReason closeReason) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Received onClose call with reason: " + closeReason.getCloseCode().getCode() + ", "
          + closeReason.getReasonPhrase());
    }
    if (onCloseMicroflowEnabled) {
      sessionManager.removeSessionAndCallCloseMicroflow(session, closeReason, onCloseMicroflow,
          onCloseMicroflowParameterKey);
    } else {
      sessionManager.removeSession(session, closeReason);
    }

  }
}