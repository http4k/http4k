description = "Http4k Standard webhooks support"

dependencies {
    api(project(":http4k-core"))
    api("dev.forkhandles:values4k:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
