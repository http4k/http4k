dependencies {
    api(project(":http4k-contract"))
    api(project(":http4k-storage-core"))
    api(project(":http4k-format-jackson"))
    testApi(project(path = ":http4k-storage-core", configuration = "testArtifacts"))
    testApi(project(":http4k-testing-hamkrest"))
}
