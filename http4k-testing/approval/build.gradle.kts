description = "Http4k support for Approval Testing"

dependencies {
    api(project(":http4k-core"))
    implementation(Testing.junit.api)
    implementation("com.natpryce:hamkrest:_")
    implementation(project(":http4k-format-jackson-yaml"))
    api("com.github.javadev:underscore:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-testing-hamkrest"))
}
