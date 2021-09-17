description = "Ultra-lightweight Selenium WebDriver implementation for http4k apps"

dependencies {
    api(project(":http4k-core"))
    api("org.seleniumhq.selenium:selenium-api:_")
    api("org.jsoup:jsoup:_")
}
