plugins {
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
    api("io.github.gradle-nexus:publish-plugin:_")
    api("com.github.jk1:gradle-license-report:_")
    api("org.jetbrains.dokka:dokka-base:_")
    api("org.jetbrains.dokka:dokka-gradle-plugin:_")
}
