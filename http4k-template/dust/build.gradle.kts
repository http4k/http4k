description = "Http4k Dust templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("org.apache.commons:commons-pool2:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-template-core", configuration = "testArtifacts"))
}
