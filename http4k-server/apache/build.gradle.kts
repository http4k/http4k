description = "Http4k HTTP Server built on top of Apache httpcore"

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents.core5:httpcore5:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
