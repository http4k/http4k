description = "Http4k Freemarker templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("org.freemarker:freemarker:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-template-core", configuration ="testArtifacts"))
}
