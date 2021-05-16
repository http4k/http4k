title: http4k WebSockets
description: An explanation of the core function types for dealing with WebSockets

**http4k** provides WebSocket support using a simple, consistent, typesafe, and testable API on supported server backends.

WebSocket communication consists of a few main concepts:

### WsMessage
As per the **http4k** ethos, an immutable message object providing duplex communication between the server to the connected client. [Lenses](/concepts/lens) can be used to provide typesafe object marshalling with WsMessages.

### WebSocket
An interface representing the available server callback API to the WebSocket channel. WebSocket objects can `send()` WsMessages to the client, or react to incoming events by binding behaviour with `onMessage()`, `onError()` or  `onClose()`. The WebSocket has a reference to the incoming [HTTP Request](/concepts/HTTP#HttpMessage) which was used during connection.

### WsConsumer
> `typealias WsConsumer = (WebSocket) -> Unit`

The primary callback received when an WebSocket server is connected to a client. API user behaviour is configured here.

### WsHandler
> `typelias WsHandler =  (Request) -> WsConsumer`

Provides the route mapping of an [HTTP Request](/concepts/HTTP#HttpMessage) to a particular WsConsumer.

### WsFilter
> `fun interface WsFilter : (WsConsumer) -> WsConsumer`

Applies decoration to a matched WsConsumer before it is invoked. WsFilters can be used to apply tangental effects to the matched WsConsumer such as logging/security, or to modify the incoming [HTTP Request](/concepts/HTTP#HttpMessage).

### WsRouter
Applies the route matching functionality when requests for WebSocket connections are received by the server.
