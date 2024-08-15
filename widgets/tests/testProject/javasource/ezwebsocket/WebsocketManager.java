package ezwebsocket;

import java.util.Map;
import java.util.HashMap;

public class WebsocketManager {
    // Map containing all registered websockets, identified by its
    // websocketidentifier/path
    private static Map<String, WebsocketEndpoint> websockets = new HashMap<String, WebsocketEndpoint>();

    public static boolean addWebsocketEndpoint(String websocketIdentifier, Long sessionTimeout, Long pingTime,
            Long pongTime) {
        if (websocketIdentifier == null || websocketIdentifier.isEmpty()) {
            throw new RuntimeException("websocketIdentifier cannot be empty");
        }
        if (sessionTimeout == null) {
            throw new RuntimeException("sessionTimeout cannot be empty");
        }
        if (pingTime == null) {
            throw new RuntimeException("pingTime cannot be empty");
        }
        if (pongTime == null) {
            throw new RuntimeException("pongTime cannot be empty");
        }
        // Create websocket handler
        try {
            WebsocketEndpoint websocketEndpoint = new WebsocketEndpoint(websocketIdentifier, sessionTimeout, pingTime,
                    pongTime);
            // Store reference to endpoint for use in notify action
            websockets.put(websocketIdentifier, websocketEndpoint);
        } catch (RuntimeException re) {
            return false;
        }
        return true;
    }

    public static boolean addWebsocketEndpointWithOnCloseMicroflow(String websocketIdentifier, Long sessionTimeout,
            Long pingTime, Long pongTime,
            String onCloseMicroflow, String onCloseMicroflowParameterKey) {
        if (websocketIdentifier == null || websocketIdentifier.isEmpty()) {
            throw new RuntimeException("websocketIdentifier cannot be empty");
        }
        if (sessionTimeout == null) {
            throw new RuntimeException("sessionTimeout cannot be empty");
        }
        if (pingTime == null) {
            throw new RuntimeException("pingTime cannot be empty");
        }
        if (pongTime == null) {
            throw new RuntimeException("pongTime cannot be empty");
        }
        if (onCloseMicroflow == null || onCloseMicroflow.isEmpty()) {
            throw new RuntimeException("onCloseMicroflow cannot be empty");
        }
        if (onCloseMicroflowParameterKey == null || onCloseMicroflowParameterKey.isEmpty()) {
            throw new RuntimeException("onCloseMicroflowParameterKey cannot be empty");
        }
        // Create websocket handler
        try {
            WebsocketEndpoint websocketEndpoint = new WebsocketEndpoint(websocketIdentifier, sessionTimeout, pingTime,
                    pongTime,
                    onCloseMicroflow,
                    onCloseMicroflowParameterKey);
            // Store reference to endpoint for use in notify action
            websockets.put(websocketIdentifier, websocketEndpoint);
        } catch (RuntimeException re) {
            return false;
        }
        return true;
    }

    public static boolean notify(String objectId, String action, String message, String websocketIdentifier) {
        if (objectId == null || objectId.isEmpty()) {
            throw new RuntimeException("objectId cannot be empty");
        }
        if ((action == null || action.isEmpty()) && (message == null || message.isEmpty())) {
            throw new RuntimeException("action and message cannot both be empty");
        }
        if (websocketIdentifier == null || websocketIdentifier.isEmpty()) {
            throw new RuntimeException("websocketIdentifier cannot be empty");
        }
        try {
            getWebsocket(websocketIdentifier).notify(objectId, action, message);
        } catch (RuntimeException re) {
            return false;
        }
        return true;
        
    }

    private static WebsocketEndpoint getWebsocket(String websocketIdentifier) {
        WebsocketEndpoint websocketEndpoint = websockets.get(websocketIdentifier);
        if (websocketEndpoint != null) {
            return websocketEndpoint;
        } else {
            throw new RuntimeException("Websocket not found for id: " + websocketIdentifier);
        }
    }

}
