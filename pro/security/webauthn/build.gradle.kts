description = "http4k Security: WebAuthn (Passkeys) support"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-security-core"))
    api(project(":http4k-connect-core"))
    api(project(":http4k-format-moshi"))
    api(libs.result4k)
    api(libs.values4k)
    api(libs.webauthn4j.core)

    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-template-freemarker"))
    testImplementation(libs.webauthn4j.test)
}
