description = "Http4k Thymeleaf templating support"

dependencies {
    api(project(":http4k-template-core"))
    api("org.thymeleaf:thymeleaf:_")
    testImplementation(testFixtures(project(":http4k-common")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}
