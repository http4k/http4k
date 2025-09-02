plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }

    api(libs.avro)
    api(libs.kotshi.api)

    testFixturesApi(libs.kotlin.reflect)

    testFixturesApi(project(":http4k-connect-kafka-rest-fake"))
    testFixturesImplementation(libs.avro)
    testFixturesApi(libs.kotshi.api)
}
