package ezwebsocket;

import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
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
  private final ILogNode LOG;

  private final long sessionTimeout;
  private final boolean enableKeepalive;
  public final long pingTime;
  public final long pongTime;

  private boolean onCloseMicroflowEnabled = false;
  private String onCloseMicroflow;
  private String onCloseMicroflowParameterKey;

  private SessionManager sessionManager;

  public WebsocketEndpoint(String websocketIdentifier, long sessionTimeout, long pingTime, long pongTime) {
    super();
    this.sessionTimeout = sessionTimeout * 1000;
    this.enableKeepalive = pingTime > 0;
    this.pingTime = pingTime * 1000;
    this.pongTime = pongTime * 1000;
    this.LOG = Core.getLogger(websocketIdentifier);
    this.sessionManager = new SessionManager(LOG, this.pingTime, this.pongTime);
    try {
      // Initialize websocket server
      Core.addWebSocketEndpoint('/' + websocketIdentifier, this);
    } catch (DeploymentException de) {
      LOG.error(de);
    }
  }

  public WebsocketEndpoint(String websocketIdentifier, long sessionTimeout, long pingTime, long pongTime,
      String onCloseMicroflow,
      String onCloseMicroflowParameterKey) {
    this(websocketIdentifier, sessionTimeout, pingTime, pongTime);
    this.onCloseMicroflowEnabled = true;
    this.onCloseMicroflow = onCloseMicroflow;
    this.onCloseMicroflowParameterKey = onCloseMicroflowParameterKey;
  }

  @Override
  public void onOpen(Session session, EndpointConfig config) {
    handleNewConnection(session);
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

  private void handleNewConnection(Session session) {
    session.setMaxIdleTimeout(sessionTimeout);
    session.addMessageHandler(new MessageHandler.Whole<String>() {
      @Override
      public void onMessage(String data) {
        try {
          if (LOG.isTraceEnabled()) {
            LOG.trace("Received message: " + data);
          }
          registerSubscription(session, data);
        } catch (RuntimeException e) {
          LOG.error("Error occured while trying to add subscription", e);
          try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, e.getMessage()));
          } catch (IOException ioe) {
            LOG.error(ioe);
          }
        }
      }
    });
    if (enableKeepalive) {
      session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
        @Override
        public void onMessage(PongMessage pongMessage) {
          try {
            if (LOG.isTraceEnabled()) {
              LOG.trace("Received pong for session " + session.getId());
            }
            sessionManager.handlePong(session);
          } catch (RuntimeException e) {
            LOG.error("Error occured while trying to handle pong", e);
            try {
              session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.getMessage()));
            } catch (IOException ioe) {
              LOG.error(ioe);
            }
          }
        }
      });
    }

  }

  private void registerSubscription(Session session, String jsonData) {
    try {
      JSONObject json = new JSONObject(jsonData);
      String objectId = json.getString("objectId");
      String csrfToken = json.getString("csrfToken");
      String onCloseMicroflowParameterValue = json.optString("onCloseMicroflowParameterValue");

      sessionManager.registerSubscription(session, csrfToken, objectId, onCloseMicroflowParameterValue);

    } catch (JSONException je) {
      throw new RuntimeException("Error occured during parsing JSONdata", je);
    } catch (RuntimeException re) {
      throw new RuntimeException("Connection refused", re);
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