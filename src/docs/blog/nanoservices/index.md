title: http4k blog: Nanoservices: The Power of Composition 
description: You thought that microservices were a thing? Pah! The powerful abstractions in the http4k toolkit allow you to write entire useful apps which fit in a Tweet.

# Nanoservices: The Power of Composition 

##### october 2020 / [@daviddenton][github]

http4k is a small library with a zero dependencies (apart from Kotlin StdLib), but what really makes it shine is the power afforded by the combination of the "Server as a Function" concepts of `HttpHandler` and `Filter`. 

Skeptical? We would be disappointed if you weren't! Hence, we decided to prove the types of things that can be accomplished with the APIs provided by http4k and a little ingenuity.

For each of the examples below, there is a fully formed http4k application declared inside a function, and the scaffolding to demonstrating it working in an accompanying `main()` using one of the swappable server backends. Even better, each of app's code (excluding import statements 🙂 ) fits in a single Tweet.

### 1. Build a simple proxy [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/simple_proxy.kt)
Requires: `http4k-core`

This simple proxy converts HTTP requests to HTTPS. Because of the symmetrical server/client `HttpHandler` signature, we can simply pipe an HTTP Client onto a server, then add a `ProxyHost` filter to do the protocol conversion.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/simple_proxy.kt"></script>

<hr/>

### 2. Report latency through a proxy [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/latency_reporting_proxy.kt)
Requires: `http4k-core`

Building on the Simple Proxy example, we can simply layer on extra filters to add features to the proxy, in this case reporting the latency of each call.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/latency_reporting_proxy.kt"></script>

<hr/>

### 3. Build a Wireshark to sniff inter-service traffic [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/wire_sniffing_proxy.kt)
Requires: `http4k-core`

Applying a `DebuggingFilter` to the HTTP calls in a proxy dumps the entire contents out to `StdOut` (or other stream).

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/wire_sniffing_proxy.kt"></script>

<hr/>

### 4. Build a ticking Websocket clock [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/websocket_clock.kt)
Requires: `http4k-core`, `http4k-server-netty`

Like HTTP handlers, Websockets in http4k can be modelled as simple functions that can be mounted onto a Server, or combined with path patterns if required.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/websocket_clock.kt"></script>

<hr/>

### 5. Build a web cache [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/disk_cache.kt)
Requires: `http4k-core`, `http4k-server-ktorcio`

Recording all traffic to disk can be achieved by just creating a `ReadWriteCache` and then adding a couple of pre-supplied Filters to a proxy. When running this example you can see that only the first request is audited.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/disk_cache.kt"></script>

<hr/>

### 6. Record all traffic to disk and replay it later [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/record_and_replay_http_traffic_proxy.kt)
Requires: `http4k-core`

This example contains two apps. The first is a proxy which captures streams of traffic and records it to a directory on disk. The second app is configured to replay the requests from that disk store at the original server. This kind of traffic capture/replay is very useful for load testing or for tracking down hard-to-diagnose bugs - and it's easy to write other other stores such as an S3 bucket etc.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/record_and_replay_http_traffic_proxy.kt"></script>

<hr/>

### 7. Watch your FS for file changes [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/file_watcher.kt)
Requires: `http4k-core`, `http4k-server-jetty`

Back to Websockets, we can watch the file system for changes and subscribe to the event feed.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/file_watcher.kt"></script>

<hr/>

### 8. Serve static files from disk [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/static_file_server.kt)
Requires: `http4k-core`, `http4k-server-undertow`

Longer than the Python `SimpleHttpServer`, but still pretty small!

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/static_file_server.kt"></script>

<hr/>

### 9. Build your own ChaosMonkey [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/chaos_proxy.kt)
Requires: `http4k-core`, `http4k-testing-chaos`

As per the [Principles of Chaos], this proxy adds Chaotic behaviour to a remote service, which is useful for modelling how a system might behave under various failure modes. Chaos can be dynamically injected via an `OpenApi` documented set of RPC endpoints.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/chaos_proxy.kt"></script>

<hr/>

### 10. Build a remote terminal! [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/web_terminal.kt)
Requires: `http4k-core`, `http4k-server-netty`

Use Websockets to remote control a terminal!* Run the example and just type commands into the prompt to have them magicked to the server backend

<sub>*Obviously this is, in general, a really (really) bad idea.</sub>

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/web_terminal.kt"></script>

<hr/>

Obviously we haven't thought of everything here. We'd love to hear your ideas about other clever uses of the http4k building blocks, or to take PRs to integrate them into the library for wider use. You can get in touch through [GitHub](http://github.com/http4k) or the usual [channels].

[github]: http://github.com/daviddenton
[http4k]: https://http4k.org
[Principles of Chaos](https://principlesofchaos.org/)
[channels]: https://www.http4k.org/support
