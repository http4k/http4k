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

    dependencies {
        testImplementation(Testing.junit.jupiter.api)
        testImplementation(Testing.junit.jupiter.engine)
        testImplementation("com.natpryce:hamkrest:_")

        testFixturesImplementation(Testing.junit.jupiter.api)
        testFixturesImplementation(Testing.junit.jupiter.engine)
        testFixturesImplementation("com.natpryce:hamkrest:_")
    }
}
