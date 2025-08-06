import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Resilience4j support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    api(platform(libs.resilience4j.bom))
    api(libs.resilience4j.bulkhead)
    api(libs.resilience4j.circuitbreaker)
    api(libs.resilience4j.ratelimiter)
    api(libs.resilience4j.retry)
    api(libs.resilience4j.timelimiter)
    testImplementation(testFixtures(project(":http4k-core")))
}

