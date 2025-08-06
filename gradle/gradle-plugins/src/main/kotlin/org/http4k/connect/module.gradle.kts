package org.http4k.connect

plugins {
    id("org.http4k.internal.module")
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

dependencies {
    api(platform("dev.forkhandles:forkhandles-bom:2.22.4.0"))
    api(project(":http4k-core"))
    api("dev.forkhandles:result4k")

    testFixturesApi(platform("org.junit:junit-bom:5.13.4"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-testing-approval"))

    testFixturesApi("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testFixturesApi(platform("org.testcontainers:testcontainers-bom:1.21.3"))
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

the<SourceSetContainer>().apply {
    named<SourceSet>("main") {
        extensions.getByName<SourceDirectorySet>("kotlin").apply {
            srcDir("build/generated/ksp/main/kotlin")
        }
    }
    named<SourceSet>("test") {
        extensions.getByName<SourceDirectorySet>("kotlin").apply {
            srcDir("src/examples/kotlin")
        }
    }

    named<SourceSet>("testFixtures") {
        extensions.getByName<SourceDirectorySet>("kotlin").apply {
            srcDir("build/generated/ksp/testFixtures/kotlin")
        }
    }
}
