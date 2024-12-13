import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Serverless support for Alibaba Function Compute"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(project(":http4k-format-moshi"))
    api("com.aliyun.fc.runtime:fc-java-core:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
