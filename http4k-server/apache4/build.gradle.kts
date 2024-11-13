description = "Http4k HTTP Server built on top of Apache httpcore"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-core"))
    api("org.apache.httpcomponents:httpcore:_")
    api("commons-codec:commons-codec:_") // override version provided by httpcore (Cxeb68d52e-5509)
    testImplementation(testFixtures(project(":http4k-core")))
}
