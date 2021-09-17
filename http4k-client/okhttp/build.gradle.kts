description = "HTTP Client built on top of okhttp"

dependencies {
    api(project(":http4k-core"))
    api("com.squareup.okhttp3:okhttp:_")
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
