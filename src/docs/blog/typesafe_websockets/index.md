# Websockets. But Testable. And Typesafe. Without the Server.

##### [@daviddenton](http://github.com/daviddenton) / december 2017

Reaction to the last post introducing [**http4k**](https://github.com/http4k/http4k) was pretty good, and one of the most popular questions was: **"But what about Websockets"**?

The answer to that question was **"Not yet"** - because they didn't fit the "Server as a Function" model, and the team hadn't worked out a way to do them in a simple, testable way. We were also worried that ignoring it might "turn out bad for us"**.

Well, a month is a long time, and we've been beavering away, so now we're thrilled to release **Websockets for [**http4k**](https://github.com/http4k/http4k)**, which are:
- **Simple**: using the same style of API as the rest of [**http4k**](https://github.com/http4k/http4k), allowing the same dynamic path-based routing as is available for standard `HttpHandlers`.
- **Typesafe**: Marshall and unmarshall typed objects from Websocket Messages using the established Lens API.
- **Testable**: This is something that is massively important to us - and just like standard HttpHandlers, [**http4k**](https://github.com/http4k/http4k) Websockets are completely testable in a synchronous offline environment. No. Server. Required.

There are 3 basic components to the API:
- A `WsHandler` - represented as a typealias: `WsHandler =  (Request) -> WsConsumer?`. This is responsible for matching an incoming HTTP upgrade request to a websocket.
- `WsConsumer` - represented as a typealias: `WsConsumer = (WebSocket) -> Unit`. This function is called on connection and allow the API user to react to events coming from the connected websocket.
- `WsMessage` - a message which is sent or received on a websocket. This message can take advantage of the typesafety accorded to other entities in http4k by using the Lens API. Just like the [**http4k**](https://github.com/http4k/http4k) HTTP message model, WsMessages are immutable data classes.

#### An example server [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_websockets/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_websockets/example.kt"></script>

Alternatively, you can check out the websocket enabled http4k [IRC clone in 30 lines of Kotlin)](https://github.com/daviddenton/http4k-demo-irc).

#### Testability [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_websockets/example_testing.kt)
As well as API simplicity, the [**http4k**](https://github.com/http4k/http4k) team are very passionate about testing, and it was very important that this could be done in an out-of-container fashion - ie. in memory and with no server being started. As such, it is possible to call `testWsClient()` on an `WsHandler` to provide a synchronous API for testing. Messages and other events can be "sent" to a connected websocket and responses will be received back in a completely predictable way from the application under test:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_websockets/example_testing.kt"></script>

#### Fin
Websocket support is now available for the Jetty server backend in [**http4k**](https://github.com/http4k/http4k) `v3.2.2`. We plan to roll out support for other server-backends in due course. Have a play a let us know what you think... 

##### Footnotes
* **We're dubious about this @kod, but are willing to meet you back at the [HackerNews](https://news.ycombinator.com/item?id=15694616) post in 25 years to find out how it went. Think of it a bit like that time we met Jarvis Cocker in the year [2000](https://www.youtube.com/watch?v=qJS3xnD7Mus). :p
