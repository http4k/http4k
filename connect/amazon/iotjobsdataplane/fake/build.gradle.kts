plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    // the shared job store record (StoredJob) lives with FakeIot, so that both fakes can be
    // constructed over the same Storage instance
    api(project(":http4k-connect-amazon-iot-fake"))

    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
