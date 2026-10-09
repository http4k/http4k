

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(project(":http4k-format-moshi"))
    api(libs.bcprov.jdk18on)

    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
