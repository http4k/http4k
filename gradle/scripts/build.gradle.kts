plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api(Kotlin.gradlePlugin)
    api(gradleApi())
    api("io.github.gradle-nexus:publish-plugin:1.3.0")
    api("com.github.jk1:gradle-license-report:_")
}
