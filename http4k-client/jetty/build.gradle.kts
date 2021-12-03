description = "HTTP Client built on top of jetty"

dependencies {
    api(project(":http4k-core"))
    api("org.eclipse.jetty:jetty-client:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
