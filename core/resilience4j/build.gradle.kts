import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k Resilience4j support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-core"))
    api("io.github.resilience4j:resilience4j-bulkhead:_")
    api("io.github.resilience4j:resilience4j-circuitbreaker:_")
    api("io.github.resilience4j:resilience4j-ratelimiter:_")
    api("io.github.resilience4j:resilience4j-retry:_")
    api("io.github.resilience4j:resilience4j-timelimiter:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
