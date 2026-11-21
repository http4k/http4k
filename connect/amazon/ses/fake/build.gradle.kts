

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    implementation(libs.jakarta.mail.api)
    runtimeOnly(libs.angus.mail)
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
