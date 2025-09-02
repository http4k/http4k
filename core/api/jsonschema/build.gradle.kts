import org.http4k.internal.ModuleLicense.Apache2

description = "http4k JSON Schema support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-format-core"))

    implementation(project(":http4k-format-jackson"))
    implementation(project(":http4k-format-kondor-json"))
    implementation(libs.values4k)
    implementation(libs.data4k)
    implementation(libs.kotlin.reflect)

    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-format-jackson"))
    testImplementation(project(":http4k-format-moshi"))
    testImplementation(testFixtures(project(":http4k-format-core")))

    testFixturesApi(project(":http4k-testing-approval"))
}

