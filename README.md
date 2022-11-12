# EZWebSocket

Allow for real-time server to client communication using websockets No third-party dependencies No runtime configuration

## Features

-   [x] Easily set up 1 or more websocket servers

-   [x] Configure 1 or more client-side actions

-   [x] Configure either limited or unlimited sessiontimeout

-   [x] Configure action to perform on sessiontimeout

-   [x] Configure action to perform on user navigating away

## Usage

### Initialize webserver

1. Add `JA_AddWebsocketEndpoint` to your afterstartup flow

2. Create a websocketidentifier, this will also be the path to the websocketserver

> I recommend storing this inside a constant with client exposure for reuse in notify action and the websocket client
> widget

3. Optionally fill in the max idle time for users to this session

### Setup client connection

#### General tab

1. Place the EZ Websocket client widget inside a context entity

2. Configure the object Id

> Note that the object database id is not available here, you will have to provide your own unique object id

3. Fill the websocket identifier with the same identifier from the websocket initialization

4. Configure one or more trigger/action combination(s)

#### Websocket close behaviour tab

**Timeout action**: If you configured a sessiontimeout during the initialization you can configure the action to perform
on sessiontimeout here

**Navigate action**: Executes action when the websocket component unrenders, examples of this are:

-   User navigates away from the page

-   User closes the page

-   The component becomes invisible

### Notify all subscribers

1. Add `JA_Notify` action to your microflow

2. Configure the object Id of which you want to notify the subscribers of

3. Fill the websocket identifier with the same identifier from the websocket initialization

4. Configure the action trigger configured in the client widget

## Issues, suggestions and feature requests

[Issues](https://github.com/nathan-JJRplus/EZWebsocket/issues)
