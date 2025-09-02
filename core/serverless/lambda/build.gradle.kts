

description = "http4k Serverless support for AWS Lambda"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-core"))
    api(libs.aws.lambda.java.core)
    compileOnly(libs.aws.lambda.java.events)
    api(project(":http4k-format-moshi")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-format-jackson"))

    testImplementation(libs.aws.lambda.java.events)

    testFixturesApi(project(":http4k-core"))
    testFixturesApi(project(":http4k-platform-core"))
    testFixturesApi(project(":http4k-config"))
    testFixturesApi(project(":http4k-serverless-lambda"))
    testFixturesApi(project(":http4k-platform-aws"))
    testFixturesApi(project(":http4k-format-jackson"))
    testFixturesApi(project(":http4k-client-okhttp"))
    testFixturesApi(project(":http4k-connect-amazon-core"))

    testFixturesApi(libs.aws.lambda.java.events)
    testFixturesApi(libs.result4k)
    testFixturesApi(testFixtures(project(":http4k-core")))
    testFixturesApi(testFixtures(project(":http4k-serverless-core")))
    testFixturesImplementation(testFixtures(project(":http4k-platform-aws")))
}
