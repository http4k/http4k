title: http4k How-to: Record and replay HTTP traffic
description: Recipes for using http4k to record and replay HTTP traffic

A set of classes to provide simple recording/replaying of HTTP traffic. This is perfect for testing purposes, or in short lived, low traffic environments where no proper caches are available.

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.10.0.0.0"
```

### Caching HTTP Traffic 

Using `Filters` it's possible to record traffic and then return recorded content instead of making repeated calls. Note that the provided storage 
implementations DO NOT have any facility for Cache Control or eviction, or respect any response headers around caching. Requests are indexed in a way optimised for retrieval.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/record_and_replay_http_traffic/example_cache.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/record_and_replay_http_traffic/example_cache.kt"></script>

### Recording Streams of HTTP Traffic 

Using `Filters` it's possible to record a stream traffic and then replay recorded content instead. Requests are indexed in a way optimised for iteration.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/record_and_replay_http_traffic/example_stream.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/record_and_replay_http_traffic/example_stream.kt"></script>

### Concepts

The `org.http4k.traffic` package contains the interfaces which make up the core concepts for traffic capture and replay. These interfaces are:

- A `Sink` consumes request/response pairs for storage. 
- A `Source` provides lookup of pre-stored Response based on an HTTP Request.
- `Replay` instances provide streams of HTTP messages as they were received.
- A `ReadWriteCache` combines `Sink` and `Source` to provide cache-like storage.
- A `ReadWriteStream` combines `Sink` and `Replay` to provide a stream of traffic which can be replayed.

The API has been designed to be modular so API users can provide their own implementations (store in S3 etc..).
