import org.http4k.internal.ModuleLicense.Apache2

description = "Ultra-lightweight Selenium WebDriver implementation for http4k apps"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.selenium.api)
    api(libs.jsoup)
    implementation(project(mapOf("path" to ":http4k-multipart")))
    testImplementation(project(mapOf("path" to ":http4k-multipart")))
}
