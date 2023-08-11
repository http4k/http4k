description = "Http4k HTTP Server built on top of Apache httpcore"

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents:httpcore:_")
    api("commons-codec:commons-codec:_") // override version provided by httpcore (Cxeb68d52e-5509)
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
