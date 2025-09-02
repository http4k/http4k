

description = "http4k Serverless support for AWS Lambda"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-lambda"))
    testImplementation(testFixtures(project(":http4k-core")))

    testImplementation(libs.aws.lambda.java.events)
}
