description = "Http4k Resilience4j support"

dependencies {
    api(project(":http4k-core"))
    api("io.github.resilience4j:resilience4j-bulkhead:_")
    api("io.github.resilience4j:resilience4j-circuitbreaker:_")
    api("io.github.resilience4j:resilience4j-ratelimiter:_")
    api("io.github.resilience4j:resilience4j-retry:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
