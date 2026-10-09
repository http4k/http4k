

description = "http4k Thymeleaf templating support"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-template-core"))
    api(libs.thymeleaf)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
