

description = "http4k Jackson JSON support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(platform(libs.jackson.bom))
    api(libs.jackson.databind)
    api(libs.jackson.module.kotlin)
    implementation(project(":http4k-api-cloudevents"))
    implementation(libs.values4k)
    implementation(libs.data4k)

    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-api-openapi"))
    testImplementation(testFixtures(project(":http4k-api-jsonschema")))
    testImplementation(project(":http4k-api-jsonrpc"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-api-openapi")))
    testImplementation(testFixtures(project(":http4k-api-jsonrpc")))
    testImplementation(project(":http4k-testing-approval"))
}
