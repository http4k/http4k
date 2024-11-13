description = "Http4k Failsafe support"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-core"))
    api("dev.failsafe:failsafe:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
