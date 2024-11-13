description = "Http4k Thymeleaf templating support"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-template-core"))
    api("org.thymeleaf:thymeleaf:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
