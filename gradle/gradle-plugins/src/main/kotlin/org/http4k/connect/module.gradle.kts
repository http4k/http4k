package org.http4k.connect

import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.`java-test-fixtures`
import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

dependencies {
    api(platform("dev.forkhandles:forkhandles-bom:_"))
    api("org.http4k:http4k-core:${rootProject.properties["http4k_version"]}")
    api("dev.forkhandles:result4k")

    testFixturesApi(platform("org.junit:junit-bom:_"))
    testFixturesApi("org.http4k:http4k-testing-hamkrest:${rootProject.properties["http4k_version"]}")
    testFixturesApi("org.http4k:http4k-testing-approval:${rootProject.properties["http4k_version"]}")

    testFixturesApi("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testFixturesApi(platform("org.testcontainers:testcontainers-bom:_"))
    testFixturesApi("org.junit.jupiter:junit-jupiter-params")
    testFixturesApi("org.testcontainers:junit-jupiter")
    testFixturesApi("org.testcontainers:testcontainers")
    testFixturesApi("dev.forkhandles:mock4k")
}


tasks {
    named<Jar>("jar") {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Vendor" to "org.http4k",
                    "Implementation-Version" to project.version
                )
            )
        }
    }
}

sourceSets {
    main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }

    test {
        kotlin.srcDir("src/examples/kotlin")
    }

    testFixtures {
        kotlin.srcDir("build/generated/ksp/testFixtures/kotlin")
        kotlin.srcDir("build/generated-testFixtures-avro-java")
    }
}
