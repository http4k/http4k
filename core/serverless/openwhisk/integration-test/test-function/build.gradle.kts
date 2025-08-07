description = "Testing against a functions deployed to Apache OpenWhisk"

plugins {
    id("org.http4k.conventions")
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":http4k-serverless-openwhisk"))
    api(testFixtures(project(":http4k-core")))
    api(testFixtures(project(":http4k-serverless-core")))
    compileOnly(libs.gson)
}
