import org.http4k.internal.ModuleLicense.Apache2
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val license by project.extra { Apache2 }

plugins {
    id("com.github.davidmc24.gradle.plugin.avro")
    id("org.http4k.module")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-format-moshi")) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }

    implementation("org.apache.avro:avro:_")
    implementation("se.ansman.kotshi:api:_")

    testFixturesApi("org.jetbrains.kotlin:kotlin-reflect")

    testFixturesApi(project(":http4k-connect-kafka-rest-fake"))
    testFixturesImplementation("org.apache.avro:avro:_")
    testFixturesApi("se.ansman.kotshi:api:_")
}

tasks {
    withType<KotlinCompile>().configureEach {
        dependsOn("generateAvroJava")
        dependsOn("generateTestFixturesAvroJava")
    }

    named("dokkaHtmlPartial", DokkaTaskPartial::class) {
        dependsOn(named("generateAvroJava"))
    }
}

