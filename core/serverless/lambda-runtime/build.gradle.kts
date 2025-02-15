import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k Serverless support for AWS Lambda"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-serverless-lambda"))
    testImplementation(testFixtures(project(":http4k-core")))

    testImplementation("com.amazonaws:aws-lambda-java-events:_")
}
