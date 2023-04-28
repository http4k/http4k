description = "Http4k KondorJson support"

dependencies {
    api(project(":http4k-format-core"))
    api("com.ubertob.kondor:kondor-core:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-contract"))
    testImplementation(project(":http4k-jsonrpc"))
    testImplementation("dev.forkhandles:values4k:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-format-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-contract", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-jsonrpc", configuration = "testArtifacts"))
}
