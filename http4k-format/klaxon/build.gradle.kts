description = "Http4k Klaxon JSON support"

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api("com.beust:klaxon:_")
    testImplementation(project(":http4k-core"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(path = ":http4k-format-core", configuration ="testArtifacts"))
    testImplementation(project(":http4k-testing-approval"))
}
