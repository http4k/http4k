import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Format supporting code"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    implementation(libs.values4k)

    testImplementation(libs.values4k)
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-api-jsonrpc"))
    testImplementation(project(":http4k-testing-hamkrest"))

    testFixturesImplementation(libs.values4k)
    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(testFixtures(project(":http4k-api-openapi")))
    testFixturesImplementation(testFixtures(project(":http4k-api-jsonrpc")))
}
