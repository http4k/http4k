description = "Machinery for running Http4k apps in cloud-native environments"

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-jackson-yaml"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(project(":http4k-format-argo"))
}
