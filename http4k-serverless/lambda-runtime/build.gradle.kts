description = "Http4k Serverless support for AWS Lambda"

dependencies {
    api(project(":http4k-serverless-lambda"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))

    testImplementation("com.amazonaws:aws-lambda-java-events:_")
}
