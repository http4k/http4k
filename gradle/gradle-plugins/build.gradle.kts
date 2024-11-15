plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
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

publishing {
    repositories {
        maven {
            name = "localHttp4kConnectRepo"
            url = file("${rootProject.projectDir}/../../../http4k-connect/gradle/repo").toURI()
        }
    }
    repositories {
        maven {
            name = "localHttp4kReleaseRepo"
            url = file("${rootProject.projectDir}/../../../http4k-release/gradle/repo").toURI()
        }
    }
}

dependencies {
    api(Kotlin.gradlePlugin)
    api(gradleApi())
    api("io.github.gradle-nexus:publish-plugin:_")
    api("com.github.jk1:gradle-license-report:_")
    api("org.jetbrains.dokka:dokka-base:_")
    api("org.jetbrains.dokka:dokka-gradle-plugin:_")
    api("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:_")
}
