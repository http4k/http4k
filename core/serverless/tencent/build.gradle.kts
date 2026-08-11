

description = "http4k Serverless support for Tencent Serverless Cloud Functions"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-format-jackson"))
}
