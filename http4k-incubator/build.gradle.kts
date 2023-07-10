description = "http4k incubator module"

dependencies {

    api(project(":http4k-core"))

    // start specmatic
    api(project(":http4k-format-jackson-yaml"))
    api("io.swagger.parser.v3:swagger-parser:2.1.16")
    api("in.specmatic:specmatic-core:0.72.0")
    compileOnly(Testing.junit.jupiter.api)
    // end specmatic

    testImplementation(project(":http4k-testing-strikt"))
    testImplementation(project(":http4k-client-apache"))
    testImplementation(project(path = ":http4k-testing-approval"))

    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
