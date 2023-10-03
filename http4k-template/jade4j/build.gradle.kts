description = "Http4k jade4j templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("de.neuland-bfi:jade4j:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
