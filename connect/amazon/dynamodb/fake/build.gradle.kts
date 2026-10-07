

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(libs.parser4k)
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
    testFixturesApi(project(path = ":http4k-connect-amazon-s3"))
}
