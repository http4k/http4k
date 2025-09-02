

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-connect-amazon-core"))
    implementation(libs.mail)

    testFixturesApi(testFixtures(project(":http4k-connect-amazon-core")))
}
