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
    implementation(Kotlin.gradlePlugin)
    api(gradleApi())

    api("com.github.jk1:gradle-license-report:_")
    runtimeOnly("io.github.gradle-nexus:publish-plugin:_")

}
