

description = "http4k support for Approval Testing"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.underscore)
    api(libs.jsoup)

    api(platform(libs.junit.bom))
    api(libs.junit.jupiter.api)
    implementation(libs.hamkrest)
    api(project(":http4k-format-jackson-yaml"))
    implementation(project(":http4k-api-cloudevents"))
    implementation(project(":http4k-web-datastar"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-testing-hamkrest"))
}
