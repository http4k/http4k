description = "Functions to be used for testing of Apache OpenWhisk"

plugins {
    id("org.http4k.conventions")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-platform-core"))
    api(project(":http4k-config"))
    api(project(":http4k-serverless-openwhisk"))
    api(project(":http4k-format-jackson"))
    api(project(":http4k-client-okhttp"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
    testImplementation("dev.forkhandles:bunting4k:_")
    testImplementation(project(":http4k-client-apache"))
    testImplementation(testFixtures(project(":http4k-core")))
}
