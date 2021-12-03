description = "Http4k WebSocket core"

dependencies {
    api(project(":http4k-core"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))   
}
