/**
 * This file was generated from EZWebsocket.xml
 * WARNING: All changes made to this file will be overwritten
 * @author Mendix UI Content Team
 */
import { CSSProperties } from "react";
import { ActionValue, DynamicValue } from "mendix";

export interface ActionConfigType {
    trigger: string;
    action?: ActionValue;
}

export interface ActionConfigPreviewType {
    trigger: string;
    action: {} | null;
}

export interface EZWebsocketContainerProps {
    name: string;
    class: string;
    style?: CSSProperties;
    tabIndex?: number;
    websocketIdentifier: DynamicValue<string>;
    objectId: DynamicValue<string>;
    actionConfig: ActionConfigType[];
    timeoutAction?: ActionValue;
    navigateAction?: ActionValue;
    onCloseMicroflowParameterValue?: DynamicValue<string>;
}

export interface EZWebsocketPreviewProps {
    className: string;
    style: string;
    styleObject?: CSSProperties;
    readOnly: boolean;
    websocketIdentifier: string;
    objectId: string;
    actionConfig: ActionConfigPreviewType[];
    timeoutAction: {} | null;
    navigateAction: {} | null;
    onCloseMicroflowParameterValue: string;
}
