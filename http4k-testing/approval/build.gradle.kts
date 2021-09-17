description = "Http4k support for Approval Testing"

dependencies {
    api(project(":http4k-core"))
    implementation("org.junit.jupiter:junit-jupiter-api:_")
    implementation("com.natpryce:hamkrest:_")
    api("com.github.javadev:underscore:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-testing-hamkrest"))
}
