import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Thymeleaf templating support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-template-core"))
    api(libs.thymeleaf)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
