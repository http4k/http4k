description = "Integration tests for all servers"

apply(plugin = "application")


dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    implementation(project(":http4k-server-apache"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-realtime-core", configuration ="testArtifacts"))
    testImplementation("com.github.docker-java:docker-java-core:_")
    testImplementation("com.github.docker-java:docker-java-transport-httpclient5:_")
}
