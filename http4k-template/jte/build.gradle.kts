description = "Http4k JTE templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("gg.jte:jte:_")
    api("gg.jte:jte-kotlin:_")

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
