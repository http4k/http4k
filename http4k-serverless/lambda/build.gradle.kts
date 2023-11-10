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

    testFixturesApi(project(":http4k-core"))
    testFixturesApi(project(":http4k-cloudnative"))
    testFixturesApi(project(":http4k-serverless-lambda"))
    testFixturesApi(project(":http4k-aws"))
    testFixturesApi(project(":http4k-format-jackson"))
    testFixturesApi(project(":http4k-client-okhttp"))

    testFixturesApi("com.amazonaws:aws-lambda-java-events:_")
    testFixturesApi("dev.forkhandles:result4k:_")
    testFixturesApi(testFixtures(project(":http4k-core")))
    testFixturesApi(testFixtures(project(":http4k-serverless-core")))
    testFixturesImplementation(testFixtures(project(":http4k-aws")))
}
