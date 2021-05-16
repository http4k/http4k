title: http4k Server-Sent Events
description: An explanation of the core function types for dealing with Server-Sent Events

**http4k** provides SSE (Server-Sent Events) support using a simple, consistent, typesafe, and testable API on supported server backends.

Sse communication consists of a few main concepts:

### SseMessage
As per the **http4k** ethos, an immutable message object to be pushed from the server to the connected client. There are 2 types of SseMessage - Events (for sending known constructs), and Data (for sending byte streams). [Lenses](/concepts/lens) can be used to provide typesafe object marshalling with SseMessages. 

### Sse
An interface representing the available server callback API to the Server-Sent Event channel. Sse objects can `send()` SseMessages to the client, or `close()` the connection. The Sse has a reference to the incoming [HTTP Request](/concepts/HTTP#HttpMessage) which was used during connection.

### SseConsumer
> `typealias SseConsumer = (Sse) -> Unit`

The primary callback received when an Sse server is connected to a client. API user behaviour is configured here.

### SseHandler
> `typelias SseHandler =  (Request) -> SseConsumer`

Provides the route mapping of an [HTTP Request](/concepts/HTTP#HttpMessage) to a particular SseConsumer.

### SseFilter
> `fun interface SseFilter : (SseConsumer) -> SseConsumer`

Applies decoration to a matched SseConsumer before it is invoked. SseFilters can be used to apply tangental effects to the matched SseConsumer such as logging/security, or to modify the incoming [HTTP Request](/concepts/HTTP#HttpMessage).

### SseRouter
Applies the route matching functionality when requests for Sse connections are received by the server.
