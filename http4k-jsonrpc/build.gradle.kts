description = "Http4k JSON-RPC support"

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}
