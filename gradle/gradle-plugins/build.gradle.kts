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
    api(gradleApi())
    api(lib("gradle-license-report"))
    api(lib("dokka-base"))
    api(lib("dokka-gradle-plugin"))
    api(lib("kotlin-gradle-plugin"))
    api(lib("ksp-gradle-plugin"))
    api(lib("gradle-maven-publish-plugin"))
}

fun lib(s: String) = the<VersionCatalogsExtension>()
    .find("libs")
    .flatMap { it.findLibrary(s) }
    .get()
