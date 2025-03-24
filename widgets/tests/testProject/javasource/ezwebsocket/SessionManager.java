package ezwebsocket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import javax.websocket.Session;
import javax.websocket.CloseReason;

public class SessionManager {

    // We have two lists, one for subscriptions, which are a combination of objectId
    // and corresponding sessions, for quick retrieval of all sessions to send a
    // notification to
    private Map<String, List<WrappedSession>> subscriptions = new HashMap<String, List<WrappedSession>>();
    // The other list is for easy retrieval of a session which has just been closed
    private Map<Session, WrappedSession> sessions = new HashMap<Session, WrappedSession>();

    private ILogNode LOG;
    private long pingTime;
    private long pongTime;

    public SessionManager(ILogNode LOG, long pingTime, long pongTime) {
        this.LOG = LOG;
        this.pingTime = pingTime;
        this.pongTime = pongTime;
        this.subscriptions = new HashMap<>();
        this.sessions = new HashMap<>();
    }

    void registerSubscription(Session session, String csrfToken, String objectId,
            String onCloseMicroflowParameterValue) {
        // Test CSRFToken for security purposes
        if (!isValidSession(csrfToken)) {
            throw new RuntimeException("Invalid csrfToken");
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding subscription: " + session.getId() + " for objectId: " + objectId);
        }

        if (sessions.containsKey(session)) {
            throw new RuntimeException("Session already registered");
        }
        
        // Create wrappedSession object and place inside objectId subscription bucket
        WrappedSession wrappedSession = new WrappedSession(session, getUserObj(csrfToken), objectId,
        		onCloseMicroflowParameterValue, pingTime, pongTime);
        addSession(wrappedSession);
    }

    void handlePong(Session session) {
        sessions.get(session).handlePong();
    }

    void notify(String objectId, List<system.proxies.User> notifyList, String payload) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Notifying subscribers of " + objectId + ": " + payload);
        }
        if (notifyList.size() == 0) {
            subscriptions.getOrDefault(objectId, Collections.emptyList())
            .forEach(subscription -> {
                try {
                    subscription.notify(payload);
                } catch (RuntimeException re) {
                    LOG.error(re);
                }
            });
        } else {
            subscriptions.getOrDefault(objectId, Collections.emptyList())
            .forEach(subscription -> {
                try {
                	if (notifyList.contains(subscription.GetUserObj())) {
                		subscription.notify(payload);
                	}                    
                } catch (RuntimeException re) {
                    LOG.error(re);
                }
            });
        }

    }

    private void addSession(WrappedSession wrappedSession) {
        subscriptions.computeIfAbsent(wrappedSession.getObjectId(), k -> new ArrayList<>()).add(wrappedSession);
        sessions.put(wrappedSession.getSession(), wrappedSession);
    }

    public WrappedSession removeSession(Session session, CloseReason closeReason) {

        WrappedSession wrappedSession = sessions.get(session);

        if (wrappedSession != null) {
            // Remove from both lists
            sessions.remove(session);

            Collection<WrappedSession> objectSubscriptions = subscriptions.get(wrappedSession.getObjectId());
            objectSubscriptions.remove(wrappedSession);

            // Check if there are no more subscriptions for objectId left, if so remove from
            // map
            if (objectSubscriptions.isEmpty()) {
                subscriptions.remove(wrappedSession.getObjectId());
            }

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

    private boolean isValidSession(String csrfToken) {
        try {
            // Check if session with this csrfToken exists
            List<system.proxies.Session> activeSessions = system.proxies.Session.load(Core.createSystemContext(),
                    String.format("[%s='%s']", system.proxies.Session.MemberNames.CSRFToken, csrfToken));
            return !activeSessions.isEmpty();

        } catch (CoreException ce) {
            throw new RuntimeException(ce);
        }
    }
    
    private system.proxies.User getUserObj(String csrfToken) {
    	try {
        	if (csrfToken.contains("'")) { // check if a malicious user is trying to escape the query with a single quote
        		throw new RuntimeException("Invalid CSRF Token. Token cannot contain a single quote.");
        	}
        	IContext context = Core.createSystemContext();
        	List<IMendixObject> userList = Core.createXPathQuery( String.format("//System.User[System.Session_User/System.Session/CSRFToken = '%s']", csrfToken))
        				.setAmount(1).execute(context);
        	if (userList.size() == 0) {
        		throw new RuntimeException("No user found for session " + csrfToken);
        	}
			return system.proxies.User.load(context, userList.get(0).getId());
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
    }

}
