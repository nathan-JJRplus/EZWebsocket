package ezwebsocket;

import java.util.HashMap;
import javax.websocket.DeploymentException;
import com.mendix.core.Core;

public class WebsocketUtils {
    // Map containing all registered websockets, identified by its
    // websocketidentifier/path
    private static HashMap<String, WebsocketEndpoint> websockets = new HashMap<String, WebsocketEndpoint>();

    public static void addWebsocketEndpoint(String websocketIdentifier, Long sessionTimeout) {
        if (websocketIdentifier.isEmpty()) {
			throw new RuntimeException("websocketIdentifier cannot be empty");
		}
		if (sessionTimeout == null) {
			throw new RuntimeException("sessionTimeout cannot be empty (use 0 for infinite sessions)");
		}
        // Create websocket handler
        WebsocketEndpoint wsEndpoint = new WebsocketEndpoint(websocketIdentifier, sessionTimeout);
        try {
            // Initialize websocket server
            Core.addWebSocketEndpoint('/' + websocketIdentifier, wsEndpoint);
        } catch (DeploymentException de) {
            wsEndpoint.LOG.error(de);
        }
        // Store reference to endpoint for use in notify action
        websockets.put(websocketIdentifier, wsEndpoint);
    }

    public static void notify(String objectId, String action, String websocketIdentifier) {
        if (objectId.isEmpty()) {
			throw new RuntimeException("objectId cannot be empty");
		}
        if (action.isEmpty()) {
			throw new RuntimeException("action cannot be empty");
		}
        if (websocketIdentifier.isEmpty()) {
			throw new RuntimeException("websocketIdentifier cannot be empty");
		}
        WebsocketEndpoint wsEndpoint = getWebsocket(websocketIdentifier);
        wsEndpoint.notify(objectId, action);
    }

    private static WebsocketEndpoint getWebsocket(String websocketIdentifier) {
        WebsocketEndpoint wsEndpoint = websockets.get(websocketIdentifier);
        if (wsEndpoint != null)
            return wsEndpoint;
        else
            throw new RuntimeException("Websocket not found for id: " + websocketIdentifier);
    }

}
