package org.http4k.connect

plugins {
    id("org.http4k.internal.module")
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

dependencies {
    api(platform(lib("forkhandles-bom")))
    api(project(":http4k-core"))
    api(lib("result4k"))

    testFixturesApi(platform(lib("junit-bom")))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-testing-approval"))

    testFixturesApi(lib("junit-jupiter-api"))
    testImplementation(lib("junit-jupiter-engine"))
    testFixturesApi(platform(lib("testcontainers-bom")))
    testFixturesApi(lib("junit-jupiter-params"))
    testFixturesApi(lib("testcontainers-junit-jupiter"))
    testFixturesApi(lib("testcontainers"))
    testFixturesApi(lib("mock4k"))
}


fun lib(s: String) = the<VersionCatalogsExtension>()
    .find("libs")
    .flatMap { it.findLibrary(s) }
    .get()


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
