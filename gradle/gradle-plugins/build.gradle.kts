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
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    api(gradleApi())
    api("com.github.jk1:gradle-license-report:2.9")
    api("org.jetbrains.dokka:dokka-base:2.0.0-Beta")
    api("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0-Beta")
    api("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.2.0-2.0.2")
    api("com.vanniktech:gradle-maven-publish-plugin:0.34.0")
}
