description = "Http4k Format supporting code"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    implementation("dev.forkhandles:values4k:2.13.4.0")

    testImplementation("dev.forkhandles:values4k:2.13.4.0")
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-jsonrpc"))
    testImplementation(project(":http4k-testing-hamkrest"))

    testFixturesImplementation("dev.forkhandles:values4k:2.13.4.0")
    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(testFixtures(project(":http4k-contract")))
    testFixturesImplementation(testFixtures(project(":http4k-jsonrpc")))
}
