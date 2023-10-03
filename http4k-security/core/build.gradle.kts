description = "Http4k Security Core support"

dependencies {
    api(project(":http4k-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}
