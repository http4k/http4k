title: http4k Nanoservices
description: Recipes useful http4k applications you can fit in a tweet

http4k is a small library with minimal dependencies, but what you can accomplish with just a single line of code is quite remarkable due to a combination of the available modules and the`Server as a Function` concept. The following applications all fit in a tweet.

### Simple Proxy [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/simple_proxy.kt)
Requires: `http4k-core`
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/simple_proxy.kt"></script>

### Latency Reporting Proxy [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/latency_reporting_proxy.kt)
Requires: `http4k-core`
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/latency_reporting_proxy.kt"></script>

### Traffic Recording Proxy & replay client [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/record_and_replay_http_traffic_proxy.kt)
Requires: `http4k-core`
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/record_and_replay_http_traffic_proxy.kt"></script>

### Static file Server [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/static_file_server.kt)
Requires: `http4k-core`
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/static_file_server.kt"></script>

### Wire-sniffing Proxy [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/simple_proxy.kt)
Requires: `http4k-core`
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/simple_proxy.kt"></script>

### Websocket Clock [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/traffic_sniffing_proxy.kt)
Requires: `http4k-core`, `http4k-server-jetty`
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/traffic_sniffing_proxy.kt"></script>

### Chaos Proxy (random latency edition) [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/latency_injection_proxy.kt)
Requires: `http4k-core`, `http4k-testing-chaos`
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nanoservices/latency_injection_proxy.kt"></script>
