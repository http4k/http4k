title: http4k Performance
description: An overview of http4k performance

The http4k server-backend modules provide a very thin adapter layer over the raw APIs of the underlying servers, so 
generally performs at a very low overhead compared to the raw server.

### Tech Empower Benchmarks
We have entered http4k into the prominent [Tech Empower Framework Benchmarks](https://www.techempower.com/benchmarks/) 
project, which assesses frameworks over a series of realistic tests. 

For this benchmark, no customisation or performance tuning of the underlying servers is done - the default application 
HttpHandler is used which is then plugged into each custom backend, as below:

```kotlin
fun main() {
    Http4kBenchmarkServer(PostgresDatabase()).start(Undertow(9000))
}
```

Command-line JVM options, however, were tuned for the test to take advantage of various JVM features.

The full implementation of the benchmark can be found [here](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Kotlin/http4k).

### Results - Round 22
Overall, http4k continues to do well in this round of benchmarking, placing 48/159 - especially considering that the [ethos](/guide/concepts/rationale) of the library is one of excellent Developer experience over and above high-end performance (which tends to result in less friendly APIs).

Rankings below are filtered for JVM libraries:

#### Composite ranking: [results](https://www.techempower.com/benchmarks/#section=data-r22&hw=ph&test=composite&l=xan3h7-cn3):
*Top rank: 13/41

#### DB query + HTML rendering: [results](https://www.techempower.com/benchmarks/#section=data-r22&hw=ph&test=fortune&l=xan3h7-cn3):
*Top rank: 25/146 - Apache backend*

Database driver used is PostgreSql backed by a Hikari pool.
Rocker templating engine used for rendering.

#### Multiple DB queries: [results](https://www.techempower.com/benchmarks/#section=data-r22&hw=ph&test=query&l=xan3h7-cn3):
*Top rank: 23/145 - Jetty Loom backend*

Database driver used is Postgres Vertx Client backed by a Hikari pool.

#### Single DB query: [results](https://www.techempower.com/benchmarks/#section=data-r22&hw=ph&test=db&l=xan3h7-cn3):
*Top rank: 25/151 - Apache backend*

Database driver used is PostgreSql backed by a Hikari pool.

#### Random DB updates: [results](https://www.techempower.com/benchmarks/#section=data-r22&hw=ph&test=update&l=xan3h7-cn3):
*Top rank: 41/138 - Jetty Loom backend*

Database driver used is Postgres Vertx Client backed by a Hikari pool.

#### JSON Serialization: [results](https://www.techempower.com/benchmarks/#section=data-r22&hw=ph&test=json&l=xan3h7-cn3):
*Top rank: 59/152 - Netty backend*

The standard Argo JSON module used for JSON creation and marshalling.

#### Plaintext pipelining: [results](https://www.techempower.com/benchmarks/#section=data-r22&hw=ph&test=plaintext&l=xan3h7-cn3):
*Top rank: 84/153 - Netty backend*

### Recommendations
Benchmark your own app's performance trying different engines if performance is critical.  The Tech Empower benchmarks attempt to simulate simple real-world scenarios, but they can behave drastically different than your app.  One other consideration is test time; some engines start up much faster than others.

[http4k]: https://http4k.org
