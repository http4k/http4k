description = "Http4k GraphQL support"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-jackson"))
    api("com.graphql-java:graphql-java:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
