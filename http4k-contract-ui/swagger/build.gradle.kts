description = "Add a locally hosted Swagger UI to your server"

dependencies {
    api(project(":http4k-contract"))
    implementation("org.webjars:swagger-ui:4.18.1")

    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}

