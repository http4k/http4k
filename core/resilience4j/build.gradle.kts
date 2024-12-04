import org.http4k.internal.ModuleLicense.Apache2

description = "DEPRECATED: Use http4k-ops-resilience4j"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    api(platform("io.github.resilience4j:resilience4j-bom:_"))
    api("io.github.resilience4j:resilience4j-bulkhead")
    api("io.github.resilience4j:resilience4j-circuitbreaker")
    api("io.github.resilience4j:resilience4j-ratelimiter")
    api("io.github.resilience4j:resilience4j-retry")
    api("io.github.resilience4j:resilience4j-timelimiter")
    testImplementation(testFixtures(project(":http4k-core")))
}
