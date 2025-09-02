

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(project(":http4k-template-pebble"))

    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
    testFixturesApi(project(":http4k-connect-amazon-sqs"))
}
