import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Serverless support for Google Cloud Functions"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(project(":http4k-format-moshi")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
    api("com.google.cloud.functions:functions-framework-api:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
