description = "http4k support for the CloudEvents format"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    api("io.cloudevents:cloudevents-core:_")
    api("io.cloudevents:cloudevents-json-jackson:_")
    api(project(":http4k-format-jackson"))
    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
    testImplementation(project(":http4k-testing-hamkrest"))
}
