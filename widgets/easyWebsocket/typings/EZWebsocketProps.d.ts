/**
 * This file was generated from EZWebsocket.xml
 * WARNING: All changes made to this file will be overwritten
 * @author Mendix Widgets Framework Team
 */
import { CSSProperties } from "react";
import { ActionValue, DynamicValue, EditableValue } from "mendix";

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
    messageAttribute?: EditableValue<string>;
    timeoutAction?: ActionValue;
    navigateAction?: ActionValue;
    onCloseMicroflowParameterValue?: DynamicValue<string>;
}

export interface EZWebsocketPreviewProps {
    /**
     * @deprecated Deprecated since version 9.18.0. Please use class property instead.
     */
    className: string;
    class: string;
    style: string;
    styleObject?: CSSProperties;
    readOnly: boolean;
    websocketIdentifier: string;
    objectId: string;
    actionConfig: ActionConfigPreviewType[];
    messageAttribute: string;
    timeoutAction: {} | null;
    navigateAction: {} | null;
    onCloseMicroflowParameterValue: string;
}
