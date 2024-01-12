description = "Http4k Standard webhooks support"

dependencies {
    api(project(":http4k-core"))
    api("dev.forkhandles:values4k:_")
    api(project(":http4k-format-core"))

    testImplementation(project(":http4k-format-jackson"))
    testImplementation(testFixtures(project(":http4k-core")))
}
