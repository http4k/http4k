description = "Http4k Jackson JSON support"

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(platform("com.fasterxml.jackson:jackson-bom:_"))
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.cloudevents:cloudevents-core:_")
    implementation("io.cloudevents:cloudevents-json-jackson:_")
    implementation("dev.forkhandles:values4k")
    implementation("dev.forkhandles:data4k")

    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-contract"))
    testImplementation(project(":http4k-jsonrpc"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-contract")))
    testImplementation(testFixtures(project(":http4k-jsonrpc")))
    testImplementation(project(":http4k-testing-approval"))
}
