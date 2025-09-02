import org.http4k.internal.ModuleLicense.Apache2

description = "http4k KondorJson support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(libs.kondor.core)
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-api-openapi"))
    testImplementation(project(":http4k-api-jsonrpc"))
    testImplementation(libs.values4k)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-api-openapi")))
    testImplementation(testFixtures(project(":http4k-api-jsonrpc")))
}
