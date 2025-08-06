description = "Testing against a functions deployed to Apache OpenWhisk"

plugins {
    id("org.http4k.conventions")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    api(project(":http4k-serverless-openwhisk"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
    compileOnly(libs.gson)
}
