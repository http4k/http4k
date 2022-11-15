description = "Http4k Format supporting code"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    implementation(project(":http4k-multipart"))
    implementation("dev.forkhandles:values4k:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-jsonrpc"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-contract", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-jsonrpc", configuration ="testArtifacts"))
}
