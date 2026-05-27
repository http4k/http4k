description = "http4k integration with the OpenFeature SDK"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-connect-openfeature"))
    api(libs.openfeature.sdk)

    testImplementation(project(":http4k-connect-openfeature-fake"))
    testImplementation(testFixtures(project(":http4k-core")))
}
