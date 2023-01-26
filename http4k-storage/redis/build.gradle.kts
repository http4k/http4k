dependencies {
    api(project(":http4k-storage-core"))
    api(project(":http4k-format-moshi"))
    api("io.lettuce:lettuce-core:6.2.2.RELEASE")

    testApi(project(path = ":http4k-storage-core", configuration = "testArtifacts"))
    testApi("org.testcontainers:junit-jupiter:1.17.6")
    testApi("org.testcontainers:testcontainers:1.17.6")
}
