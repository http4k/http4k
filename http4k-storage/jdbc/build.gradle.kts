dependencies {
    api(project(":http4k-storage-core"))
    api("org.jetbrains.exposed:exposed-jdbc:_")

    testApi(project(path = ":http4k-storage-core", configuration = "testArtifacts"))
    testApi(project(":http4k-format-moshi"))
    testApi("com.zaxxer:HikariCP:_")
    testApi("com.h2database:h2:_")
}
