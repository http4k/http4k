description = "Http4k Serverless support for AWS Lambda"

dependencies {
    api(project(":http4k-serverless-core"))
    api("com.amazonaws:aws-lambda-java-core:_")
    compileOnly("com.amazonaws:aws-lambda-java-events:_")
    api(project(":http4k-format-moshi")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-format-jackson"))

    testImplementation("com.amazonaws:aws-lambda-java-events:_")
}
