title: http4k blog: Websockets. But typesafe. And testable. Without the Server.
description: An overview of typesafe Websocket support in http4k

# Websockets. But typesafe. And testable. Without the Server.

##### december 2017 / [@daviddenton][github]

Reaction to the last post introducing http4k was pretty good, and one of the most popular questions was: **"But what about Websockets"**?

The answer to that question at the time was an emphatic **"Not yet"** - because they didn't fit the "Server as a Function" model, and the team hadn't worked out a way to deliver them in a simple, offline testable&ast; way.

Well, a month is a long time, and we've been beavering away, so now we're thrilled to release **Websockets for http4k**, which are:

- **Simple**: using the same style of API as the rest of http4k, allowing the same dynamic path-based routing as is available for standard `HttpHandlers`.
- **Typesafe**: Marshall and unmarshall typed objects from Websocket Messages using the established Lens API.
- **Testable**: This is something that is massively important to us - and just like standard HttpHandlers, http4k Websockets are completely testable in a synchronous online or offline environment. No. Server. Required.

#### Details schmeetails...

Just as with HttpHandlers, the here are 2 basic function types which make up the core of the Websocket routing API:

- A `WsHandler` - represented as a typealias: `(Request) -> WsConsumer?`. This is responsible for matching an incoming HTTP upgrade request to a websocket.
- `WsConsumer` - represented as a typealias: `(WebSocket) -> Unit`. This function is called on connection and allow the API user to react to events coming from the connected Websocket by attaching listeners.

Additionally, `WsMessage` objects are used for actual communication - ie. a message which is sent or received on a Websocket. This message can take advantage of the typesafety accorded to other entities in http4k by using the Lens API. And just like the http4k HTTP message model, WsMessages are **immutable data classes**.

#### An example server [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_websockets/example.kt)
The example below shows how:
 
- Websockets can be dynamically routed
- Lens-based marshalling of Websocket message objects using Jackson. 
- `WsHandler` can be combined with an `HttpHandler` to make a `PolyHandler` - an application which can serve many protocols. Conversion of the `PolyHandler` to a supporting Server can be done via the standard `asServer()` mechanism, or it can be kept offline for ultra-fast in-memory testing:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_websockets/example.kt"></script>

Alternatively, you can check out the Websocket enabled http4k demo: [IRC clone in 30 lines of Kotlin](https://github.com/daviddenton/http4k-demo-irc).

#### Testability [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_websockets/example_testing.kt)
As well as API simplicity, the http4k team are very passionate about testing, and it was very important that this could be done in an out-of-container fashion - ie. in memory and with no server being started. As such, it is possible to call `testWsClient()` on an `WsHandler` to provide a synchronous API for testing. Messages and other events can be "sent" to a connected websocket and responses will be received back in a completely predictable way from the application under test.

In the below example, we have gone one step further - defining a contract test case and then providing 2 implementations of it - one for unit-testing (in memory), one using a server. http4k provides clients with an identical interface for both cases, meaning it's possible reuse the same test logic:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/typesafe_websockets/example_testing.kt"></script>

#### Fin

Websocket support is now available for the Jetty server backend in http4k `v3.2.0`. We plan to roll out support for other server-backends in due course. Have a play a let us know what you think... 

##### Footnotes
* &ast; We had a bit of a search for "unit testing websockets", half through curiosity and half because we wanted to swipe other people's ideas for implementing it. But we came up with nothing - it seems like all the existing JVM HTTP libraries rely on running servers for testing websockets. We hope we're wrong - because the alternative makes us a little &lt;insert sadface emoji/&gt;. If we are, then please let us know! 😝

[github]: http://github.com/daviddenton
[http4k]: https://http4k.org
