description = "Http4k Serverless support for Alibaba Function Compute"

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(project(":http4k-format-moshi"))
    api("com.aliyun.fc.runtime:fc-java-core:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
