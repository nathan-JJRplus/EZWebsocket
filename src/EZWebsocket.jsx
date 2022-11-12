import { createElement, useEffect, useRef } from "react";

export function EZWebsocket({ objectId, websocketEndpoint, actionConfig, timeoutAction, navigateAction }) {
    // Persist connection throughout render cycles
    const connection = useRef(null);

    useEffect(() => {
        // Check if there is no open connection already
        connection.current === null &&
            // Make sure all values are initiated
            objectId.status === "available" &&
            websocketEndpoint.status === "available" &&
            startConnection();
    }, [objectId, websocketEndpoint]);

    useEffect(() => {
        return () => {
            // Close connection on unmount
            connection.current.close();
        };
    }, []);

    const startConnection = () => {
        // Open websocket connection
        // The replace action makes sure that applications without ssl connect to ws:// and with ssl connect to wss://
        let ws = new WebSocket(mx.appUrl.replace(/http/, "ws") + websocketEndpoint.value);

        ws.onopen = event => {
            // Send objectId to wsserver on opening of connection
            // to connect the current session to the object
            ws.send(objectId.value);
        };

        ws.onmessage = event => {
            // Find the action to execute for the received triggerstring
            let config = actionConfig.find(config => {
                return config.trigger === event.data;
            });
            if (!config) {
                console.log("Action " + event.data + " not implemented");
                return;
            }
            console.debug("Execute action: " + event.data);
            config.action && config.action.canExecute
                ? config.action.execute()
                : console.error("Action " + event.data + " could not be executed");
        };

        ws.onclose = event => {
            console.debug(event);
            // Timeout event
            event.code === 1001 && timeoutAction && timeoutAction.canExecute && timeoutAction.execute();
            // Navigate away/close page/unrender event
            event.code === 1005 && navigateAction && navigateAction.canExecute && navigateAction.execute();
        };

        // Store connection inside ref so we can keep track through rendercycles
        connection.current = ws;
    };
    return null;
}
