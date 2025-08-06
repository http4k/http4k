package org.http4k

import org.gradle.api.JavaVersion.VERSION_21
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
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

the<KotlinJvmProjectExtension>().apply {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val sourcesJar by tasks.registering(Jar::class, fun Jar.() {
    archiveClassifier.set("sources")
    from(project.the<SourceSetContainer>()["main"].allSource)
    dependsOn(tasks.named("classes"))
})

val javadocJar by tasks.registering(Jar::class, fun Jar.() {
    archiveClassifier.set("javadoc")
    from(tasks.named<Javadoc>("javadoc").get().destinationDir)
    dependsOn(tasks.named("javadoc"))
})

val testJar by tasks.registering(Jar::class, fun Jar.() {
    archiveClassifier.set("test")
    from(project.the<SourceSetContainer>()["test"].output)
})

the<SourceSetContainer>().apply {
    named<SourceSet>("test") {
        extensions.getByName<SourceDirectorySet>("kotlin").apply {
            srcDir("$projectDir/src/examples/kotlin")
            srcDir("$projectDir/src/tools/kotlin")
        }
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
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
            freeCompilerArgs.add("-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled")
        }
    }

    the<JavaPluginExtension>().apply {
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
                freeCompilerArgs.add("-Xannotation-default-target=param-property")
            }
        }
    }
}

dependencies {

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.natpryce:hamkrest:1.8.0.1")

    testFixturesApi(platform("org.junit:junit-bom:5.13.4"))
    testFixturesImplementation("org.junit.platform:junit-platform-launcher")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-engine")
    testFixturesImplementation("com.natpryce:hamkrest:1.8.0.1")
}
