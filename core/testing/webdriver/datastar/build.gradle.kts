description = "Datastar-enabled WebDriver implementation for http4k apps"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-testing-webdriver"))
    api(project(":http4k-web-datastar"))
    api(project(":http4k-format-moshi"))
    api(libs.parser4k)
    compileOnly(libs.jspecify)
}
