description = "Http4k Pebble templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("io.pebbletemplates:pebble:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
