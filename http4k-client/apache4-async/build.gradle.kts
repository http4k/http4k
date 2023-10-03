description = "Http4k HTTP Client built on top of async apache httpclient"

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents:httpasyncclient:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
