package org.http4k

import gradle.kotlin.dsl.accessors._2c21a9b74f632bc62548adf44b0b8067.java
import gradle.kotlin.dsl.accessors._2c21a9b74f632bc62548adf44b0b8067.kotlin
import gradle.kotlin.dsl.accessors._2c21a9b74f632bc62548adf44b0b8067.sourceSets
import gradle.kotlin.dsl.accessors._2c21a9b74f632bc62548adf44b0b8067.test
import gradle.kotlin.dsl.accessors._2c21a9b74f632bc62548adf44b0b8067.testFixturesApi
import gradle.kotlin.dsl.accessors._2c21a9b74f632bc62548adf44b0b8067.testFixturesImplementation
import gradle.kotlin.dsl.accessors._2c21a9b74f632bc62548adf44b0b8067.testImplementation
import org.gradle.api.JavaVersion.VERSION_21
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    idea
    `java-library`
    `java-test-fixtures`
}

repositories {
    mavenCentral()
    mavenLocal()
}

version = rootProject.version

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(project.the<SourceSetContainer>()["main"].allSource)
    dependsOn(tasks.named("classes"))
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named<Javadoc>("javadoc").get().destinationDir)
    dependsOn(tasks.named("javadoc"))
}

val testJar by tasks.creating(Jar::class) {
    archiveClassifier.set("test")
    from(project.the<SourceSetContainer>()["test"].output)
}

sourceSets {
    test {
        kotlin.srcDir("$projectDir/src/examples/kotlin")
        kotlin.srcDir("$projectDir/src/tools/kotlin")
    }
}

tasks {
    named<Jar>("jar") {
        manifest {
            val projectName = rootProject.name.replace('-', '_')
            attributes(mapOf("${projectName}_version" to archiveVersion))
        }
    }

    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JVM_21)
        }
    }

    java {
        sourceCompatibility = VERSION_21
        targetCompatibility = VERSION_21
    }

    withType<Test> {
        useJUnitPlatform()
        jvmArgs = listOf("--enable-preview")
    }

    withType<GenerateModuleMetadata> {
        enabled = false
    }

    named<KotlinJvmCompile>("compileTestKotlin").configure {
        if (name == "compileTestKotlin") {
            compilerOptions {
                jvmTarget.set(JVM_21)
                freeCompilerArgs.add("-Xjvm-default=all")
            }
        }
    }
}

dependencies {

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.natpryce:hamkrest:_")

    testFixturesApi(platform("org.junit:junit-bom:_"))
    testFixturesImplementation("org.junit.platform:junit-platform-launcher")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-engine")
    testFixturesImplementation("com.natpryce:hamkrest:_")
}
