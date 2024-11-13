import org.http4k.internal.ModuleLicense.Apache2

description = "Http4k Serverless support for AWS Lambda"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.module")
}


dependencies {
    api(project(":http4k-serverless-lambda"))
    testImplementation(testFixtures(project(":http4k-core")))

    testImplementation("com.amazonaws:aws-lambda-java-events:_")
}
