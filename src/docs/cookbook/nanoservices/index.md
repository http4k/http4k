title: http4k Nanoservices
description: Recipes useful http4k applications you can fit in a tweet

http4k is a small library with minimal dependencies, but what you can accomplish with just a single line of code is quite remarkable due to a combination of the available modules and the `Server as a Function` concept.

The main code of the following http4k applications (in the appropriately named function) all fit in a tweet (140 characters)... exports excluded ;)

### Simple Proxy [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/simple_proxy.kt)
Requires: `http4k-core`

This simple proxy converts HTTP requests to HTTPS. Because of the symmetrical server/client HttpHandler signature, we can simply mount an HTTP client onto a Server, then add a `ProxyHost` filter to do the protocol conversion.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/simple_proxy.kt"></script>

### Latency Reporting Proxy [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/latency_reporting_proxy.kt)
Requires: `http4k-core`

Building on the Simple Proxy example, we can simply layer on extra filters to add features to the proxy, in this case reporting the latency of each call.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/latency_reporting_proxy.kt"></script>

### Wire-sniffing Proxy [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/wire_sniffing_proxy.kt)
Requires: `http4k-core`

Applying a `DebuggingFilter` to the HTTP calls in a proxy dumps the entire contents out to `StdOut` (or other stream).

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/wire_sniffing_proxy.kt"></script>

### Traffic Recording Proxy & Replayer [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/record_and_replay_http_traffic_proxy.kt)
Requires: `http4k-core`

This example contains two apps. The first is a proxy which captures streams of traffic and records it to a directory on disk. The second app is configured to replay the requests from that disk store at the original server. This kind of traffic capture/replay is very useful for load testing or for tracking down hard-to-diagnose bugs - and it's easy to write other other stores such as an S3 bucket etc.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/record_and_replay_http_traffic_proxy.kt"></script>

### Static file Server [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/static_file_server.kt)
Requires: `http4k-core`

Longer than the Python `SimpleHttpServer`, but still pretty small!

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/static_file_server.kt"></script>

### Websocket Clock [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/websocket_clock.kt)
Requires: `http4k-core`, `http4k-server-jetty`

Like Http handlers, Websockets in http4k can be modelled as simple functions that can be mounted onto a Server, or combined with path patterns if required.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/websocket_clock.kt"></script>

### Chaos Proxy (random latency edition) [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/chaos_proxy.kt)
Requires: `http4k-core`, `http4k-testing-chaos`

As per the [Principles of Chaos](https://principlesofchaos.org/), this proxy adds Chaotic behaviour to a remote service, which is useful for modelling how a system might behave under various failure modes. Chaos can be dynamically injected via an `OpenApi` documented set of RPC endpoints.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/chaos_proxy.kt"></script>
