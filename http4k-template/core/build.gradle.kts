description = "Http4k templating core"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))   
}
