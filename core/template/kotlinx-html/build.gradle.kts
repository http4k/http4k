description = "http4k kotlinx-html templating support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-template-core"))
    api(libs.kotlinx.html.jvm)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-approval"))
}
