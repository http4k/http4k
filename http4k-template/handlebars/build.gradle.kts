description = "Http4k Handlebars templating support"

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-template-core"))
    api("com.github.jknack:handlebars:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
