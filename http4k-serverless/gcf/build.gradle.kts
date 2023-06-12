description = "Http4k Serverless support for Google Cloud Functions"

dependencies {
    api(project(":http4k-serverless-core"))
    api(project(":http4k-format-moshi")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
    api("com.google.cloud.functions:functions-framework-api:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
