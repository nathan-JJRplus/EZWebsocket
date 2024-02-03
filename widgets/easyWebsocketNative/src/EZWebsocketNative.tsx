import { MutableRefObject, useEffect, useRef } from "react";
import { TextStyle, ViewStyle } from "react-native";

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
        if (
            connection.current === null &&
            // Make sure all values are initiated
            objectId.status === "available" &&
            websocketIdentifier.status === "available" &&
            (!messageAttribute || messageAttribute.status === "available") &&
            (!onCloseMicroflowParameterValue || onCloseMicroflowParameterValue.status === "available")
        ) {
            startConnection();
        }
    }, [objectId, websocketIdentifier, messageAttribute, onCloseMicroflowParameterValue]);

    useEffect(() => {
        return () => {
            // Close connection on unmount
            connection.current?.close();
        };
    }, []);

    const startConnection = () => {
        // Open websocket connection
        // The replace action makes sure that applications without ssl connect to ws:// and with ssl connect to wss://
        const ws = new WebSocket(global.mx.remoteUrl.replace(/http/, "ws") + websocketIdentifier.value);

        ws.onopen = _event => {
            // Send objectId, csrftoken and onCloseMicroflowParamterValue to wsserver on opening of connection
            // to connect the current session to the object
            const parameters = {
                objectId: objectId.value,
                csrfToken: global.mx.session.sessionData.csrftoken,
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
            // Timeout event
            if (event.code === 1001 && timeoutAction && timeoutAction.canExecute) {
                timeoutAction.execute();
            }
            // Navigate away/close page/unrender event
            if (event.code === 1005 && navigateAction && navigateAction.canExecute) {
                navigateAction.execute();
            }
        };

        // Store connection inside ref so we can keep track through rendercycles
        connection.current = ws;

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

    return null;
}
