description = "Http4k Freemarker templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("org.freemarker:freemarker:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
