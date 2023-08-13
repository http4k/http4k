description = "Http4k JTE templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("gg.jte:jte:_")
    api("gg.jte:jte-kotlin:_")

    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-template-core", configuration = "testArtifacts"))
}
