title: http4k Structured Logging with Events
description: Recipe for using http4k Events to send structured logs to external log sinks

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "3.216.0"
    compile group: "org.http4k", name: "http4k-format-jackson", version: "3.216.0"
```

In order to leverage modern log aggregation platforms, we should move away from logging arbitrary strings into the StdOut of our applications, and move towards [Structured Logging](https://www.thoughtworks.com/radar/techniques/structured-logging) instead, which allows us to treat logs as data which can be mined to give us better observability of our systems. This also encourages the move for developers to think about which events happening in your apps are actually important and what data is appropriate to be attached to each one.

**http4k** supports Structured Logging using a simple yet powerful concept - an `Event` is simply a marker interface that can be attached to any class, which we then send to an instance of `Events` (a "sink" for sending `Event` instances to). As with the `HttpHandler`, `Events` is just a typealias of `(Event) -> Unit`, and similarly to the `HttpHandler`, an Event can be transformed or decorated with metadata using an `EventFilter` (modelled as `(Events) -> Events`)).

Support for leveraging auto "object to JSON" transformational capabilities is included for the libraries that have it (eg. Jackson and GSON). This allows custom `Json` instances to be used (for instance) to avoid PII information being spat out to log aggregation platforms where they could be masked using the configuration of the JSON renderer.

Attaching metadata to an `Event` results in (compactified) JSON similar to this:
```json
{
  "event": {
    "uri": "/path1",
    "status": 200,
    "duration": 16
  },
  "metadata": {
    "timestamp": "2019-11-05T17:32:27.297448Z",
    "traces": {
      "traceId": "e35304c95b704c7d",
      "spanId": "0e46f7b3cb5bcf2e",
      "parentSpanId": null,
      "samplingDecision": "1"
    },
    "requestCount": 1234
  }
}
```

In harmony with the ethos of **http4k** there is no need to bring in a custom logging library such as SL4J, although they would be very simple to integrate if required by implementing a custom `Events` instance.

The example below shows a simple application that outputs structured logs to StdOut which can be analysed by an aggregator, along with the attachment of extra `Event` metadata via a custom `EventFilter`.
 
### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/structured_logging_with_events/example.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/structured_logging_with_events/example.kt"></script>
