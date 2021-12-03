description = "Http4k Pebble templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("io.pebbletemplates:pebble:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-template-core", configuration ="testArtifacts"))
}
