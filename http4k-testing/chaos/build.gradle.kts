description = "Http4k support for chaos testing"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-contract"))
    api(project(":http4k-format-jackson"))
    testImplementation(project(":http4k-testing-approval"))

    testImplementation(testFixtures(project(":http4k-core")))
}
