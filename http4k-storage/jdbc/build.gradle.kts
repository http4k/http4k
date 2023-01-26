dependencies {
    api(project(":http4k-storage-core"))
    api("org.jetbrains.exposed:exposed-jdbc:0.41.1")

    testApi(project(path = ":http4k-storage-core", configuration = "testArtifacts"))
    testApi(project(":http4k-format-moshi"))
    testApi("com.zaxxer:HikariCP:5.0.1")
    testApi("com.h2database:h2:2.1.214")
}
