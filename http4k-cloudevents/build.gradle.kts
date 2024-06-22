description = "http4k support for the CloudEvents format"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    api("io.cloudevents:cloudevents-core:_")
    api("io.cloudevents:cloudevents-json-jackson:_")
    api("com.fasterxml.jackson.datatype:jackson-datatype-guava") // for CVE workaround (guava)
    api(project(":http4k-format-jackson"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-testing-hamkrest"))
}
