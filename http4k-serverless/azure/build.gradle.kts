description = "Http4k Serverless support for Azure Functions"

dependencies {
    api(project(":http4k-serverless-core"))
    api("com.microsoft.azure.functions:azure-functions-java-library:_")
    testImplementation(testFixtures(project(":http4k-core")))
}


