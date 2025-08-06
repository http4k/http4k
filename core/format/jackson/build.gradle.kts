import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Jackson JSON support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(platform(libs.jackson.bom))
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(libs.cloudevents.core)
    implementation(libs.cloudevents.json.jackson)
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
