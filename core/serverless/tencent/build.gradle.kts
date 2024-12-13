import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Serverless support for Tencent Serverless Cloud Functions"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api("com.tencentcloudapi:scf-java-events:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
