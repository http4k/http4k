import org.http4k.internal.ModuleLicense.Apache2

description = "Ultra-lightweight Selenium WebDriver implementation for http4k apps"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api("org.seleniumhq.selenium:selenium-api:_")
    api("org.jsoup:jsoup:_")
    implementation(project(mapOf("path" to ":http4k-multipart")))
    testImplementation(project(mapOf("path" to ":http4k-multipart")))
}
