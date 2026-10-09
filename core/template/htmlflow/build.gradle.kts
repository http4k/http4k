description = "http4k HtmlFlow templating support"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-template-core"))
    api(libs.htmlflow)
    api(libs.htmlflow.view.loader)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
