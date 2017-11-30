# Websockets. But Testable. And Typesafe.

##### [@daviddenton](http://github.com/daviddenton) / december 2017

### Wat?

Reaction to the last post introducing [**http4k**](https://github.com/http4k/http4k) was pretty good, and one of the most popular questions was: **"But what about Websockets"**?

The answer to that question was **Not yet** - because they didn't fit the "Server as a Function" model, and we hadn't worked out a way to do them in a simple, testable way.

Well, a month is a long time, and we've been beavering away, so now we're thrilled to release **Websockets for [**http4k**](https://github.com/http4k/http4k)**, which are:
- **Simple**: using the same style of API as the rest of [**http4k**](https://github.com/http4k/http4k), allowing the same dynamic path-based routing as is available for standard `HttpHandlers`.
- **Typesafe**: Marshall and unmarshall typed objects from Websocket Messages using the established  Lens API.
- **Testable**: This is something that is massively important to us - and just like standard HttpHandlers, [**http4k**](https://github.com/http4k/http4k) Websockets are completely testable in a synchronous offline environment. No. Server. Required.

### Concepts
- A `WsHandler` - represented as a typealias: `WsHandler =  (Request) -> WsConsumer?`. This is responsible for matching an incoming HTTP upgrade request to a websocket.
- `WsConsumer` - represented as a typealias: `WsConsumer = (WebSocket) -> Unit`. This function is called on connection of a websocket and allow the API user to react to events coming from the connected websocket.
- `WsMessage` - a message which is sent or received on a websocket. This message can take advantage of the typesafety accorded to other entities in http4k by using the Lens API.

#### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/hamcrest/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/hamkrest/example.kt"></script>
