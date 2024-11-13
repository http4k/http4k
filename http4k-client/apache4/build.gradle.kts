description = "Http4k HTTP Client built on top of apache-httpclient"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents:httpclient:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
