description = "Http4k Security JWT support"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-security-core"))
    api("com.nimbusds:nimbus-jose-jwt:_")
    testImplementation(testFixtures(project(":http4k-core")))
}
