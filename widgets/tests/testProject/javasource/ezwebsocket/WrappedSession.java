package ezwebsocket;

import javax.websocket.Session;

public class WrappedSession {
    public Session session;
    private String objectId;
    private String onCloseMicroflowParameterValue;

    public WrappedSession(Session session, String objectId, String onCloseMicroflowParameterValue) {
        this.session = session;
        this.objectId = objectId;
        this.onCloseMicroflowParameterValue = onCloseMicroflowParameterValue;
    }

    public Session getSession() {
        return this.session;
    }

    public String getObjectId() {
        return this.objectId;
    }

    public String getOnCloseMicroflowParameterValue() {
        return this.onCloseMicroflowParameterValue;
    }

    public void notify(String payload) {
        this.session.getAsyncRemote().sendText(payload);
    }
}
