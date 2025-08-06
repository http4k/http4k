import org.http4k.internal.ModuleLicense.Apache2

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api("org.apache.avro:avro:_")

    testFixturesApi(libs.kotshi.api)

    testFixturesApi(project(":http4k-connect-kafka-schemaregistry"))
}
