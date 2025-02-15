import org.http4k.internal.ModuleLicense.Http4kCommercial

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    api("org.apache.avro:avro:_")

    testFixturesApi("se.ansman.kotshi:api:_")

    testFixturesApi(project(":http4k-connect-kafka-schemaregistry"))
}
