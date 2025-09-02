

description = "http4k Serverless support for Tencent Serverless Cloud Functions"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(libs.tencent.scf.java.events)
    testImplementation(testFixtures(project(":http4k-core")))
}
