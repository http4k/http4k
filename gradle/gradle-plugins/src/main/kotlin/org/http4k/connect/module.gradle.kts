package org.http4k.connect

import gradle.kotlin.dsl.accessors._e5ae26700a26e172740719aa2f29fd91.api
import gradle.kotlin.dsl.accessors._e5ae26700a26e172740719aa2f29fd91.kotlin
import gradle.kotlin.dsl.accessors._e5ae26700a26e172740719aa2f29fd91.main
import gradle.kotlin.dsl.accessors._e5ae26700a26e172740719aa2f29fd91.sourceSets
import gradle.kotlin.dsl.accessors._e5ae26700a26e172740719aa2f29fd91.test
import gradle.kotlin.dsl.accessors._e5ae26700a26e172740719aa2f29fd91.testFixtures
import gradle.kotlin.dsl.accessors._e5ae26700a26e172740719aa2f29fd91.testFixturesApi
import gradle.kotlin.dsl.accessors._e5ae26700a26e172740719aa2f29fd91.testImplementation
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.`java-test-fixtures`
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.repositories

plugins {
    id("org.http4k.internal.module")
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

dependencies {
    api(platform("dev.forkhandles:forkhandles-bom:_"))
    api(project(":http4k-core"))
    api("dev.forkhandles:result4k")

    testFixturesApi(platform("org.junit:junit-bom:_"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-testing-approval"))

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
