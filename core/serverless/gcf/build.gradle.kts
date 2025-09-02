import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Serverless support for Google Cloud Functions"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(project(":http4k-format-moshi")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
    api(libs.gcp.functions.framework.api)
    testImplementation(testFixtures(project(":http4k-core")))
}
