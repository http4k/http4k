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
    api(Kotlin.gradlePlugin)
    api(gradleApi())
    api("com.github.jk1:gradle-license-report:_")
    api("org.jetbrains.dokka:dokka-base:_")
    api("org.jetbrains.dokka:dokka-gradle-plugin:_")
    api("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:_")
    api("com.vanniktech:gradle-maven-publish-plugin:0.34.0")
}
