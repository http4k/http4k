import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k A2A server SDK"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-mcp-core"))
    api(project(":http4k-mcp-client"))

    testImplementation(project(":http4k-serverless-lambda"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-helidon"))
    testImplementation(project(":http4k-testing-approval"))

    // TODO REMOVE?
    testImplementation(project(":http4k-security-core"))
}
