title: http4k WebSockets
description: An explanation of the core function types for dealing with WebSockets

**http4k** provides WebSocket support using a simple, consistent, typesafe, and testable API on supported server backends.

WebSocket communication consists of a few main concepts:

### WsMessage
As per the **http4k** ethos, an immutable message object providing duplex communication between the server to the connected client. [Lenses](/guide/concepts/lens) can be used to provide typesafe object marshalling with WsMessages.

### WebSocket
```kotlin
interface Websocket {
    val upgradeRequest: Request
    fun send(message: WsMessage)
    fun close(status: WsStatus = NORMAL)
    fun onError(fn: (Throwable) -> Unit)
    fun onClose(fn: (WsStatus) -> Unit)
    fun onMessage(fn: (WsMessage) -> Unit)
}
```

An interface representing the available server callback API to the WebSocket channel. WebSocket objects can `send()` WsMessages to the client, or react to incoming events by binding behaviour with `onMessage()`, `onError()` or  `onClose()`. The WebSocket has a reference to the incoming [HTTP Request](/guide/concepts/http#HttpMessage) which was used during connection.

### WsConsumer
```kotlin
typealias WsConsumer = (WebSocket) -> Unit
```

The primary callback received when an WebSocket server is connected to a client. API user behaviour is configured here.

### WsHandler
```kotlin
typealias WsHandler = (Request) -> WsConsumer
```

Provides the route mapping of an [HTTP Request](/guide/concepts/http#HttpMessage) to a particular WsConsumer.

### WsFilter
```kotlin
fun interface WsFilter : (WsConsumer) -> WsConsumer
```

Applies decoration to a matched WsConsumer before it is invoked. WsFilters can be used to apply tangental effects to the matched WsConsumer such as logging/security, or to modify the incoming [HTTP Request](/guide/concepts/http#HttpMessage).

### WsRouter
```kotlin
interface WsRouter {
    fun match(request: Request): WsRouterMatch
    fun withBasePath(new: String): WsRouter
    fun withFilter(new: WsFilter): WsRouter
}
```
Applies the route matching functionality when requests for WebSocket connections are received by the server.
