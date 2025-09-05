description = "http4k HtmlFlow templating support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-template-core"))
    api(libs.htmlflow)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
