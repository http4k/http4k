plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
    api(project(":http4k-connect-storage-core"))
    api(libs.kotshi.api)

    testImplementation(project(":http4k-format-moshi"))
    testImplementation(project(":http4k-connect-openfeature-fake"))
    testImplementation(testFixtures(project(":http4k-core")))
    testFixturesApi(testFixtures(project(":http4k-connect-core")))
}
