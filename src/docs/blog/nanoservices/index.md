title: http4k blog: Nanoservices: The Power of composition 
description: You thought that microservices were a thing? Pah! The powerful abstractions in the http4k toolkit allow you to write entire useful apps which fit in a Tweet.

# Nanoservices: The Power of Composition 

##### october 2020 / [@daviddenton][github]

http4k is a small library with a minimal dependency set, but what really makes it shine is the power afforded by the combination of the "Server as a Function" concepts of `HttpHandler` and `Filter`. 

Skeptical? We would be disappointed if you weren't! Hence, we decided to prove the types of things that can be accomplished with the APIs provided by [http4k] and a little ingenuity.

For each of the examples below, there is a fully formed [http4k] applications declared inside a function, and the scaffolding to demonstrating it working in an accompanying `main()`. Even better, all of the main app code (excluding import statements 🙂) can fit in a single Tweet.

### 1. Build a simple proxy [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/simple_proxy.kt)
Requires: `http4k-core`

This simple proxy converts HTTP requests to HTTPS. Because of the symmetrical server/client `HttpHandler` signature, we can simply pipe an HTTP Client onto a Server, then add a `ProxyHost` filter to do the protocol conversion.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/simple_proxy.kt"></script>

<hr/>

### 2. Report latency through a proxy [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/latency_reporting_proxy.kt)
Requires: `http4k-core`

Building on the Simple Proxy example, we can simply layer on extra filters to add features to the proxy, in this case reporting the latency of each call.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/latency_reporting_proxy.kt"></script>

<hr/>

### 3. Build a cheap Wireshark to sniff inter-service traffic [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/wire_sniffing_proxy.kt)
Requires: `http4k-core`

Applying a `DebuggingFilter` to the HTTP calls in a proxy dumps the entire contents out to `StdOut` (or other stream).

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/wire_sniffing_proxy.kt"></script>

<hr/>

### 4. Record all traffic to disk and replay it later [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/record_and_replay_http_traffic_proxy.kt)
Requires: `http4k-core`

This example contains two apps. The first is a proxy which captures streams of traffic and records it to a directory on disk. The second app is configured to replay the requests from that disk store at the original server. This kind of traffic capture/replay is very useful for load testing or for tracking down hard-to-diagnose bugs - and it's easy to write other other stores such as an S3 bucket etc.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/record_and_replay_http_traffic_proxy.kt"></script>

<hr/>

### 5. Serve static files from disk [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/static_file_server.kt)
Requires: `http4k-core`

Longer than the Python `SimpleHttpServer`, but still pretty small!

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/static_file_server.kt"></script>

<hr/>

### 6. Build a ticking Websocket clock [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/websocket_clock.kt)
Requires: `http4k-core`, `http4k-server-netty`

Like HTTP handlers, Websockets in http4k can be modelled as simple functions that can be mounted onto a Server, or combined with path patterns if required.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/websocket_clock.kt"></script>

<hr/>

### 7. Build your own ChaosMonkey [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/chaos_proxy.kt)
Requires: `http4k-core`, `http4k-testing-chaos`

As per the [Principles of Chaos](https://principlesofchaos.org/), this proxy adds Chaotic behaviour to a remote service, which is useful for modelling how a system might behave under various failure modes. Chaos can be dynamically injected via an `OpenApi` documented set of RPC endpoints.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/chaos_proxy.kt"></script>


### 8. Watch for file changes [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/file_watcher.kt)
Requires: `http4k-core`, `http4k-server-jetty`

Back to Websockets, we can watch the file system for changes and subscribe to the event feed.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/blog/nanoservices/file_watcher.kt"></script>

<hr/>
[github]: http://github.com/daviddenton
[http4k]: https://http4k.org
