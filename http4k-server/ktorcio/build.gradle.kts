description = "Http4k HTTP Server built on top of Ktor CIO engine"

dependencies {
    api(project(":http4k-core"))
    api("io.ktor:ktor-server-cio:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
