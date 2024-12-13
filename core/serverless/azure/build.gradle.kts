import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "http4k Serverless support for Azure Functions"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api("com.microsoft.azure.functions:azure-functions-java-library:_")
    testImplementation(testFixtures(project(":http4k-core")))
}


