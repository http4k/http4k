import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Klaxon JSON support"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api("com.beust:klaxon:_")
    testImplementation(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(project(":http4k-testing-approval"))
}
