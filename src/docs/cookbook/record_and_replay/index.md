A set of classes to provide simple recording/replaying of HTTP traffic. This is perfect for testing purposes, or in environments where no proper caches are available.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "2.29.0"
```

### Caching HTTP Traffic 

Using `Filters` it's possible to record traffic and then return recorded content instead of making repeated calls. Note that the provided storage 
implementations DO NOT have any facility for Cache Control or eviction, or respect any response headers around caching. Requests are indexed in a way optimised for retrieval.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/record_and_replay/example_cache.kt"></script>

### Recording Streams of HTTP Traffic 

Using `Filters` it's possible to record a stream traffic and then replay recorded content instead. Requests are indexed in a way optimised for iteration.


<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/record_and_replay/example_stream.kt"></script>
