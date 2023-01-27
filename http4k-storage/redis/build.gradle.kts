dependencies {
    api(project(":http4k-storage-core"))
    api(project(":http4k-format-moshi"))
    api("io.lettuce:lettuce-core:_")

    testApi(project(path = ":http4k-storage-core", configuration = "testArtifacts"))
    testApi(platform("org.testcontainers:testcontainers-bom:_"))
    testApi("org.testcontainers:junit-jupiter")
    testApi("org.testcontainers:testcontainers")
}
