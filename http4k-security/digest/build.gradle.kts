description = "Http4k Security Digest support"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-security-core"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))   
}
