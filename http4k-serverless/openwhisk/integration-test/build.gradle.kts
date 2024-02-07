description = "Functions to be used for testing of Apache OpenWhisk"

apply(plugin = "com.github.johnrengelman.shadow")

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-cloudnative"))
    api(project(":http4k-serverless-openwhisk"))
    api(project(":http4k-format-jackson"))
    api(project(":http4k-client-okhttp"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
    testImplementation("dev.forkhandles:bunting4k:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
