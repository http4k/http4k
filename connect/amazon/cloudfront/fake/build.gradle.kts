

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(project(":http4k-template-pebble"))
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
