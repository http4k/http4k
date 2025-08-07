plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "org.http4k"
version = "0.0.0.0"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin") {
        version {
            strictly(libs.versions.kotlin.get())
        }
    }
    api(gradleApi())
    api(lib("gradle-license-report"))
    api(lib("dokka-base"))
    api(lib("dokka-gradle-plugin"))
    api("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin") {
        version {
            strictly(libs.versions.com.google.devtools.ksp.get())
        }
    }
    api(lib("gradle-maven-publish-plugin"))
}

fun lib(s: String) = the<VersionCatalogsExtension>()
    .find("libs")
    .flatMap { it.findLibrary(s) }
    .get()
