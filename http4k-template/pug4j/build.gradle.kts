description = "Http4k Pug4j templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("de.neuland-bfi:pug4j:_")
    testImplementation(testFixtures(project(":http4k-common")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
