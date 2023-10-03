description = "Http4k HTTP Client built on top of apache-httpclient"

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents.client5:httpclient5:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
