description = "Http4k HTTP Client built on top of async apache httpclient"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
}

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents:httpasyncclient:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
