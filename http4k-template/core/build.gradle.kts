description = "Http4k templating core"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}
