description = "Http4k Security Digest support"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-security-core"))
    testImplementation(testFixtures(project(":http4k-core")))
}
