description = "Http4k Serverless support for AWS Lambda"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
    id("org.http4k.code-coverage")
}

dependencies {
    api(project(":http4k-serverless-lambda"))
    testImplementation(testFixtures(project(":http4k-core")))

    testImplementation("com.amazonaws:aws-lambda-java-events:_")
}
