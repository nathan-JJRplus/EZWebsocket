package ezwebsocket;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.List;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import javax.websocket.Session;
import javax.websocket.CloseReason;

public class SessionManager {

    // We have two lists, one for subscriptions, which are a combination of objectId
    // and corresponding sessions, for quick retrieval of all sessions to send a
    // notification to
    private final Map<String, List<WrappedSession>> subscriptions = new ConcurrentHashMap<String, List<WrappedSession>>();
    // The other list is for easy retrieval of a session which has just been closed
    private final Map<Session, WrappedSession> sessions = new ConcurrentHashMap<Session, WrappedSession>();

    private ILogNode LOG;
    private long pingTime;
    private long pongTime;

    public SessionManager(ILogNode LOG, long pingTime, long pongTime) {
        this.LOG = LOG;
        this.pingTime = pingTime;
        this.pongTime = pongTime;
    }

    void registerSubscription(Session session, String objectId,
            String onCloseMicroflowParameterValue) {

        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding subscription: " + session.getId() + " for objectId: " + objectId);
        }

        if (sessions.containsKey(session)) {
            throw new RuntimeException("Session already registered");
        }
        // Create wrappedSession object and place inside objectId subscription bucket
        WrappedSession wrappedSession = new WrappedSession(session, objectId, onCloseMicroflowParameterValue, pingTime,
                pongTime);
        addSession(wrappedSession);
    }

    void handlePong(Session session) {
        WrappedSession wrappedSession = sessions.get(session);
        if (wrappedSession != null) {
            wrappedSession.handlePong();
        }
    }

    void notify(String objectId, String payload) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Notifying subscribers of " + objectId + ": " + payload);
        }
        subscriptions.getOrDefault(objectId, Collections.emptyList())
                .forEach(subscription -> {
                    try {
                        subscription.notify(payload);
                    } catch (RuntimeException re) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("RuntimeException while sending to subscriber: " + re.getMessage());
                        }
                    }
                });
    }

    private void addSession(WrappedSession wrappedSession) {
        subscriptions.computeIfAbsent(wrappedSession.getObjectId(), k -> new CopyOnWriteArrayList<>())
                .add(wrappedSession);
        sessions.put(wrappedSession.getSession(), wrappedSession);
    }

    public WrappedSession removeSession(Session session, CloseReason closeReason) {

        WrappedSession wrappedSession = sessions.remove(session);

        if (wrappedSession != null) {
            // Remove from both lists
            subscriptions.computeIfPresent(wrappedSession.getObjectId(), (key, list) -> {
                list.remove(wrappedSession);
                return list.isEmpty() ? null : list;
            });
        }
        return wrappedSession;
    }

    public void removeSessionAndCallCloseMicroflow(Session session, CloseReason closeReason, String onCloseMicroflow,
            String onCloseMicroflowParameterKey) {
        WrappedSession wrappedSession = removeSession(session, closeReason);
        if (wrappedSession != null && onCloseMicroflow != null && !onCloseMicroflow.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Scheduling onCloseMicroflow:" + onCloseMicroflow + " with parametervalue: "
                        + wrappedSession.getOnCloseMicroflowParameterValue());
            }
            Core.microflowCall(onCloseMicroflow)
                    .withParam(onCloseMicroflowParameterKey, wrappedSession.getOnCloseMicroflowParameterValue())
                    .executeInBackground(Core.createSystemContext(), "EZWebsocket.TQ_OnCloseMicroflowCall");
        }
    }
}
