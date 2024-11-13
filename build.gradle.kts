import org.gradle.api.JavaVersion.VERSION_1_8
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.http4k.nexus-config")
    id("org.http4k.conventions")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(Kotlin.gradlePlugin)
        classpath("org.openapitools:openapi-generator-gradle-plugin:_")
        classpath("org.jetbrains.kotlin:kotlin-serialization:_")
        classpath("gradle.plugin.com.github.johnrengelman:shadow:_")
    }
}

allprojects {
    apply(plugin = "org.http4k.conventions")
    apply(plugin = "org.http4k.code-coverage")

    repositories {
        mavenCentral()
    }

    version = project.properties["releaseVersion"] ?: "LOCAL"
    group = "org.http4k"

    tasks {
        withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JVM_1_8)
            }
        }

        java {
            sourceCompatibility = VERSION_1_8
            targetCompatibility = VERSION_1_8
        }

        withType<Test> {
            useJUnitPlatform()
            jvmArgs = listOf("--enable-preview")
        }

        withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    dependencies {
        testImplementation(Testing.junit.jupiter.api)
        testImplementation(Testing.junit.jupiter.engine)
        testImplementation("com.natpryce:hamkrest:_")

        testFixturesImplementation(Testing.junit.jupiter.api)
        testFixturesImplementation(Testing.junit.jupiter.engine)
        testFixturesImplementation("com.natpryce:hamkrest:_")
    }
}

dependencies {
    subprojects
        .filter { hasAnArtifact(it) }
        .forEach {
            api(project(it.name))
            testImplementation(testFixtures(project(it.name)))
        }
}

fun hasAnArtifact(it: Project) = !it.name.contains("test-function") && !it.name.contains("integration-test")

sourceSets {
    test {
        kotlin.srcDir("$projectDir/src/docs")
        resources.srcDir("$projectDir/src/docs")
    }
}

tasks.register("listProjects") {
    doLast {
        subprojects
            .filter { hasAnArtifact(it) }
            .forEach { System.err.println(it.name) }
    }
}
