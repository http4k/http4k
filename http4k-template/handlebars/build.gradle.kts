description = "Http4k Handlebars templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("com.github.jknack:handlebars:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-template-core", configuration = "testArtifacts"))
}
