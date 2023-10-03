description = "Http4k HTTP Server built on top of Ratpack"

dependencies {
    api(project(":http4k-core"))
    api("io.ratpack:ratpack-core:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
