description = "Http4k JSON-RPC support"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))   
}
