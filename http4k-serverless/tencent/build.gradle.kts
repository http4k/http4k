description = "Http4k Serverless support for Tencent Serverless Cloud Functions"

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api("com.tencentcloudapi:scf-java-events:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
