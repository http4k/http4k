The **http4k** server-backend modules provide a very thin adapter layer over the raw APIs of the underlying servers, so 
should perform at a very low overhead compared to the raw server.

### Tech Empower Benchmarks
We have entered **http4k** into the prominent [Tech Empower Framework Benchmarks](https://www.techempower.com/benchmarks/) 
project, which assesses frameworks over a series of realistic tests, including:

* JSON response processing
* Random data-updates (database)
* Random data reads (database)
* Template-rendering (HTML)
* Plain-text pipelining

For this benchmark, no customisation or performance tuning of the underlying servers was done - the default Server 
construction mechanic was used, as below:

```kotlin
fun main(args: Array<String>) {
    Http4kBenchmarkServer.start(Netty(9000))
}
```

Command-line JVM options, however, were tuned for the test to take advantage of various JVM features.

The full implementation of the benchmark can be found [here](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Kotlin/http4k).

### Results
Results and analysis will be posted here when the next round (R15) of the benchmarks is published.
