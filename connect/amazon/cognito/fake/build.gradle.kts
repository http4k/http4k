

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(project(":http4k-security-oauth"))
    api(libs.jose4j)

    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
