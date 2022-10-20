description = "Http4k support for Approval Testing"

dependencies {
    api(project(":http4k-core"))
    api("com.github.javadev:underscore:_")

    implementation(Testing.junit.jupiter.api)
    implementation("com.natpryce:hamkrest:_")
    implementation(project(":http4k-format-jackson-yaml"))
    implementation(project(":http4k-cloudevents"))

    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-testing-hamkrest"))
}
