description = "Http4k Datastar support utilities"

plugins {
    id("org.http4k.conventions")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("dev.forkhandles:values4k:_")

    testImplementation(testFixtures(project(":http4k-core")))
}
