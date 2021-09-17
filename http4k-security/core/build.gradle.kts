description = "Http4k Security Core support"

dependencies {
    api(project(":http4k-core"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))   
}
