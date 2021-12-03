description = "Http4k HTTP Server built on top of Apache httpcore"

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents:httpcore:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
