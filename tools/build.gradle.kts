plugins {
    id("org.http4k.conventions")
}

dependencies {
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-template-handlebars"))
    testImplementation(project(":http4k-testing-webdriver"))
    testImplementation(project(":http4k-client-apache"))
    testImplementation("com.lemonappdev:konsist:_")
}
