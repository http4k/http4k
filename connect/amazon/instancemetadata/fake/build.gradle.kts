

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    testFixturesApi(project(path = ":http4k-connect-amazon-sns-fake"))
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
