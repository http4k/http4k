description = "http4k incubator: Storyboard"

plugins {
    id("org.http4k.conventions")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-moshi"))
    api(project(":http4k-testing-webdriver"))
    api(project(":http4k-template-freemarker"))

    compileOnly(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter.api)

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(":http4k-testing-hamkrest"))
}
