

description = "http4k Argo JSON support"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-format-core"))
    api(libs.argo)
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-api-openapi"))
    testImplementation(project(":http4k-api-jsonrpc"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-api-openapi")))
    testImplementation(testFixtures(project(":http4k-api-jsonrpc")))
}
