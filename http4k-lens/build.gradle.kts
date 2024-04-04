description = "Dependency-lite Server as a Function in pure Kotlin"

dependencies {
    api(project(":http4k-common"))
    implementation("dev.forkhandles:values4k:_")
    implementation("dev.forkhandles:result4k:_")

    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-client-websocket"))
}
