

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(libs.jakarta.mail.api)
    api(libs.angus.mail)
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
