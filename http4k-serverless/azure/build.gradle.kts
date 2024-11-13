description = "Http4k Serverless support for Azure Functions"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api("com.microsoft.azure.functions:azure-functions-java-library:_")
    testImplementation(testFixtures(project(":http4k-core")))
}


