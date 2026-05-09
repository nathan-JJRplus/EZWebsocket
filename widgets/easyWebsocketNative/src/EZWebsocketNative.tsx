import { MutableRefObject, useEffect, useRef } from "react";
import { TextStyle, ViewStyle, AppState, AppStateStatus } from "react-native";

import { Style } from "@mendix/pluggable-widgets-tools";

import { EZWebsocketNativeProps } from "../typings/EZWebsocketNativeProps";

declare global {
    var mx: any; // eslint-disable-line no-var
}

export interface CustomStyle extends Style {
    container: ViewStyle;
    label: TextStyle;
}

export function EZWebsocketNative({
    objectId,
    websocketIdentifier,
    actionConfig,
    messageAttribute,
    timeoutAction,
    navigateAction,
    onCloseMicroflowParameterValue
}: EZWebsocketNativeProps<CustomStyle>) {
    // Persist connection throughout render cycles
    const connection: MutableRefObject<WebSocket | null> = useRef(null);

    useEffect(() => {
        // Check if there is no open connection already
        if (connection.current === null && canStartConnection()) {
            startConnection();
        }
    }, [objectId, websocketIdentifier, messageAttribute, onCloseMicroflowParameterValue, actionConfig]);

    useEffect(() => {
        // Add EventListener for AppState changes to be able to reconnect when the app comes back from background and connection was silently closed
        const sub = AppState.addEventListener("change", (nextState: AppStateStatus) => {
            console.debug(`AppState change: ${nextState} | wsRef=${connection.current ? "set" : "null"}`);
            if (nextState === "active") {
                const ws = connection.current;
                const rs = ws?.readyState;

                if (rs === WebSocket.OPEN || rs === WebSocket.CONNECTING || !canStartConnection()) {
                    return;
                }

                try {
                    ws?.close();
                } catch {
                    console.error("Error while closing websocket connection");
                }
                connection.current = null;
                startConnection();
            }
        });

        return () => sub.remove();
    }, [objectId, websocketIdentifier, messageAttribute, onCloseMicroflowParameterValue, actionConfig]);

    useEffect(() => {
        return () => {
            // Close connection on unmount
            connection.current?.close();
            connection.current = null;
        };
    }, []);

    const startConnection = () => {
        // Extra guardrail
        const rs = connection.current?.readyState;
        if (rs === WebSocket.OPEN || rs === WebSocket.CONNECTING) {
            return;
        }
        // Open websocket connection
        console.debug(`Starting connection: ${websocketIdentifier.value} - ${objectId.value}`);
        // The replace action makes sure that applications without ssl connect to ws:// and with ssl connect to wss://
        const ws = new WebSocket(global.mx.remoteUrl.replace(/http/, "ws") + websocketIdentifier.value);
        // Store connection inside ref so we can keep track through rendercycles
        connection.current = ws;

        ws.onopen = _event => {
            // Send objectId, csrftoken and onCloseMicroflowParamterValue to wsserver on opening of connection
            // to connect the current session to the object
            const parameters = {
                objectId: objectId.value,
                onCloseMicroflowParameterValue: onCloseMicroflowParameterValue?.value
            };
            ws.send(JSON.stringify(parameters));
        };

        ws.onmessage = event => {
            // eventdata looks like this:
            // {
            //    "action": "<actiontrigger>",
            //    "message": "<message>"
            // }
            const payload = JSON.parse(event.data);
            setMessage(payload.message);
            executeAction(payload.action);
        };

        ws.onclose = event => {
            console.debug(event);
            // Timeout event (server-side ping/pong detected dead connection)
            if (
                (event.code === 1001 || event.code === 1006 || event.code === 1008 || event.code === 1011) &&
                timeoutAction &&
                timeoutAction.canExecute
            ) {
                timeoutAction.execute();
            }

            if (event.code === 1005 && navigateAction && navigateAction.canExecute) {
                navigateAction.execute();
            }

            if (connection.current === ws) {
                connection.current = null;
            }
        };

        const executeAction = (action: string) => {
            if (!action) {
                return;
            }
            // Find the action to execute for the received triggerstring
            const config = actionConfig.find(config => {
                return config.trigger === action;
            });
            if (!config) {
                console.log("Action " + action + " not implemented");
                return;
            }
            console.debug("Execute action: " + action);
            if (config.action && config.action.canExecute) {
                config.action.execute();
            } else {
                console.error("Action " + action + " could not be executed");
            }
        };

        const setMessage = (message: string) => {
            if (!message) {
                return;
            }
            if (!messageAttribute) {
                console.debug("messageAttribute not set");
                return;
            }
            if (messageAttribute?.readOnly) {
                console.debug("cannot set messageAttribute, as it is readOnly");
                return;
            }
            messageAttribute.setValue(message);
        };
    };

    const canStartConnection = () => {
        return (
            objectId.status === "available" &&
            websocketIdentifier.status === "available" &&
            (!messageAttribute || messageAttribute.status === "available") &&
            (!onCloseMicroflowParameterValue || onCloseMicroflowParameterValue.status === "available") &&
            (!actionConfig ||
                !actionConfig.find(config => {
                    return config.action?.canExecute === false; // This check ensures parameters from Datasource flows are available in actions
                }))
        );
    };

    return null;
}
