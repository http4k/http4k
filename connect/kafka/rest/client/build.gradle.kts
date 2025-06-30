import org.http4k.internal.ModuleLicense.Apache2
import org.jetbrains.dokka.gradle.DokkaTaskPartial

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }

    api("org.apache.avro:avro:_")
    api("se.ansman.kotshi:api:_")

    testFixturesApi("org.jetbrains.kotlin:kotlin-reflect")

    testFixturesApi(project(":http4k-connect-kafka-rest-fake"))
    testFixturesImplementation("org.apache.avro:avro:_")
    testFixturesApi("se.ansman.kotshi:api:_")
}
