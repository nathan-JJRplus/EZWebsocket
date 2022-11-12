package ezwebsocket;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.RemoteEndpoint.Async;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

public class WebsocketEndpoint extends Endpoint {
  public ILogNode LOG;
  private long sessionTimeout;

  public WebsocketEndpoint(String websocketIdentifier, long sessionTimeout) {
    super();
    this.sessionTimeout = sessionTimeout;
    this.LOG = Core.getLogger(websocketIdentifier);
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
  private HashMap<Session, String> subscriptions = new HashMap<Session, String>();

  private void addSubscription(Session session, String objectId) {
    if (LOG.isTraceEnabled())
      LOG.trace("Adding subscription: " + session.getId() + " for objectId: " + objectId);
    subscriptions.put(session, objectId);
  }

  private void removeSubscription(Session session) {
    if (LOG.isTraceEnabled())
      LOG.trace("Removing subscription: " + session.getId());
    subscriptions.remove(session);
  }

  // Retrieve all sessions subscribed to given object
  private Set<Async> getSubscriptions(String objectId) {
    Set<Async> remoteSet = new HashSet<Async>();
    subscriptions.forEach((key, value) -> {
      if (value.equals(objectId)) {
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
      public void onMessage(String objectId) {
        // The only message the server expects is the message sent from the widget on
        // connection open
        // which is the objectId to subscribe the session to
        if (objectId != null) {
          addSubscription(session, objectId);
        } else {
          try {
            session.close();
          } catch (IOException e) {
            if (LOG.isDebugEnabled())
              LOG.debug(e.toString());
          }

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

}