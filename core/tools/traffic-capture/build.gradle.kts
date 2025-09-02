

description = "http4k Tools for HTTP Traffic Capture/Playback"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    compileOnly(project(":http4k-connect-storage-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-connect-storage-core"))
}
