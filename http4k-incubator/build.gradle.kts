description = "http4k incubator module"

dependencies {
    api(project(":http4k-core"))

    testImplementation(project(":http4k-testing-strikt"))
    testImplementation(project(":http4k-client-apache"))
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-testing-approval"))
    testImplementation(project(path = ":http4k-contract", configuration = "testArtifacts"))
}
