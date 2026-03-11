plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
    api(project(":http4k-security-core"))
    api(libs.kotshi.api)
    api(libs.values4k)
    testFixturesApi(testFixtures(project(":http4k-connect-core")))
    testImplementation(testFixtures(project(":http4k-core")))
}
