description = "Http4k jade4j templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("de.neuland-bfi:jade4j:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-template-core", configuration = "testArtifacts"))
}
