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
    mavenLocal()
    val actor = System.getenv("GITHUB_ACTOR")
    val token = System.getenv("GITHUB_TOKEN")
    if (actor != null && token != null) {
        maven {
            name = "http4kStandards"
            url = uri("https://maven.pkg.github.com/http4k/*")
            credentials {
                username = actor
                password = token
            }
        }
    }
}

dependencies {
    api(gradleApi())
    api(lib("http4k-standards"))
    api(lib("gradle-license-report"))
    api(lib("dokka-base"))
    api(lib("dokka-gradle-plugin"))
    api(lib("kotlin-gradle-plugin"))
    api(lib("ksp-gradle-plugin"))
    api(lib("gradle-maven-publish-plugin"))
    api(lib("spotless-gradle-plugin"))
    api(lib("cyclonedx-gradle-plugin"))
    api(lib("detekt-gradle-plugin"))
}

fun lib(s: String) = the<VersionCatalogsExtension>()
    .find("libs")
    .flatMap { it.findLibrary(s) }
    .get()
