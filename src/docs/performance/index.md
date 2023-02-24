title: http4k Performance
description: An overview of http4k performance

The http4k server-backend modules provide a very thin adapter layer over the raw APIs of the underlying servers, so 
generally performs at a very low overhead compared to the raw server.

### Tech Empower Benchmarks
We have entered http4k into the prominent [Tech Empower Framework Benchmarks](https://www.techempower.com/benchmarks/) 
project, which assesses frameworks over a series of realistic tests. 

For this benchmark, no customisation or performance tuning of the underlying servers was done - the default application 
HttpHandler was used which is then plugged into each custom backend, as below:

```kotlin
fun main() {
    Http4kBenchmarkServer.start(Undertow(9000))
}
```

Command-line JVM options, however, were tuned for the test to take advantage of various JVM features.

The full implementation of the benchmark can be found [here](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Kotlin/http4k).

### Results - Round 19
Overall, http4k did very well in this round of benchmarking, placing 19/104 - especially considering that the [ethos](/guide/concepts/rationale) of the library is one of excellent Developer experience over and above high-end performance (which tends to result in less friendly APIs).

The big surprise was the high performance of the Apache server backend, which consistently outranked Undertow (which is the most fully featured of all the supported backends and our default option). 

#### Overall ranking (a: [results](https://www.techempower.com/benchmarks/#section=data-r19&hw=ph&test=composite):
*Top rank: 9/29

#### DB query + HTML rendering: [results](https://www.techempower.com/benchmarks/#section=data-r19&hw=ph&test=fortune&l=fjd30b):
*Top rank: 9/111 - Apache backend*

Database driver used is PostgreSql backed by a Hikari pool.
Handlebars templating engine used for rendering.

#### Multiple DB queries: [results](https://www.techempower.com/benchmarks/#section=data-r19&hw=ph&test=query&l=fjd30b):
*Top rank: 10/105 - Undertow backend*

Database driver used is PostgreSql backed by a Hikari pool.

#### Single DB query: [results](https://www.techempower.com/benchmarks/#section=data-r19&hw=ph&test=db&l=fjd30b):
*Top rank: 22/111 - Apache backend*

Database driver used is PostgreSql backed by a Hikari pool.

#### Random DB updates: [results](https://www.techempower.com/benchmarks/#section=data-r19&hw=ph&test=update&l=fjd30b):
*Top rank: 23/93 - Undertow backend*

Database driver used is PostgreSql backed by a Hikari pool.

#### JSON Serialization: [results](https://www.techempower.com/benchmarks/#section=data-r19&hw=ph&test=json&l=fjd30b):
*Top rank: 35/110 - Apache backend*

The standard Jackson module used for JSON creation and marshalling.

#### Plaintext pipelining: [results](https://www.techempower.com/benchmarks/#section=data-r19&hw=ph&test=plaintext&l=fjd30b):
*Top rank: 55/113 - Apache backend*

### Recommendations
Benchmark your own app's performance trying different engines if performance is critical.  The Tech Empower benchmarks attempt to simulate simple real-world scenarios, but they can behave drastically different than your app.  One other consideration is test time; some engines start up much faster than others.

[http4k]: https://http4k.org
