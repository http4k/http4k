dependencies {
    api(project(":http4k-format-moshi"))
    testApi(project(path = ":http4k-storage-core", configuration = "testArtifacts"))

//    implementation(project(":http4k-connect-amazon-s3"))
//    testImplementation(project(path = ":http4k-connect-amazon-s3", configuration = "testArtifacts"))
//    testImplementation(project(":http4k-connect-amazon-s3-fake"))
}
