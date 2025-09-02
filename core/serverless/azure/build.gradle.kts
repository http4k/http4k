import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Serverless support for Azure Functions"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(libs.azure.functions.java.library)
    testImplementation(testFixtures(project(":http4k-core")))
}


