

description = "http4k Moshi JSON support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(libs.moshi)
    api(libs.moshi.kotlin)

    api(project(":http4k-api-jsonschema"))
    implementation(libs.values4k)
    implementation(libs.data4k)

    testImplementation(project(":http4k-core"))
    testImplementation(libs.moshi)

    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(":http4k-testing-strikt"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
    testImplementation(testFixtures(project(":http4k-api-jsonschema")))
}
