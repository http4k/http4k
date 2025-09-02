

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api(libs.avro)

    testFixturesApi(libs.kotshi.api)

    testFixturesApi(project(":http4k-connect-kafka-schemaregistry"))
}
