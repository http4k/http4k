

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    testFixturesApi(project(":http4k-connect-amazon-route53-fake"))
    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
