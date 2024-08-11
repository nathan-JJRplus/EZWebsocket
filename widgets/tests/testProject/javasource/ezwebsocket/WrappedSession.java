package ezwebsocket;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.websocket.CloseReason;
import javax.websocket.Session;

public class WrappedSession {
    public final Session session;
    private final String objectId;
    private final String onCloseMicroflowParameterValue;
    
    private final AtomicBoolean pongReceived;
    private final Timer pingTimer;
    private Timer pongTimer;

    public WrappedSession(Session session, String objectId, String onCloseMicroflowParameterValue) {
        this.session = session;
        this.objectId = objectId;
        this.onCloseMicroflowParameterValue = onCloseMicroflowParameterValue;
        this.pingTimer = new Timer(true);
        this.pongReceived = new AtomicBoolean(true);
        startPingTimer();
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

    private void startPingTimer() {
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!pongReceived.get()) {
                    closeSession();
                } else {
                    pongReceived.set(false);
                    session.getAsyncRemote().sendText("ping");
                    startPongTimeout();
                }
            }
        }, 0, 5000); // 30-second interval
    }

    private void startPongTimeout() {
        if (pongTimer != null) {
            pongTimer.cancel();
        }
        pongTimer = new Timer(true);
        pongTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!pongReceived.get()) {
                    closeSession();
                }
            }
        }, 10000);
    }

    public void handlePong() {
        pongReceived.set(true);
        cancelPongTimeout();
    }

    private void cancelPongTimeout() {
        if (pongTimer != null) {
            pongTimer.cancel();
        }
    }

    private void closeSession() {
        try {
            pingTimer.cancel();
            session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Pong not received"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
