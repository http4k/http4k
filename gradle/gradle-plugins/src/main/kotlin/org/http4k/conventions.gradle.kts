package org.http4k

import org.gradle.api.JavaVersion.VERSION_17
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
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
            jvmTarget.set(JVM_17)
        }
    }

    java {
        sourceCompatibility = VERSION_17
        targetCompatibility = VERSION_17
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
                jvmTarget.set(JVM_17)
                freeCompilerArgs.add("-Xjvm-default=all")
            }
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:_")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:_")
    testImplementation("com.natpryce:hamkrest:_")

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:_")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-engine:_")
    testFixturesImplementation("com.natpryce:hamkrest:_")
}
