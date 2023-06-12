import groovy.namespace.QName
import groovy.util.Node
import org.gradle.api.JavaVersion.VERSION_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration

plugins {
    kotlin("jvm")
    idea
    jacoco
    `java-library`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:_")
        classpath("org.openapitools:openapi-generator-gradle-plugin:_")
        classpath("org.jetbrains.kotlin:kotlin-serialization:_")
        classpath("gradle.plugin.com.github.johnrengelman:shadow:_")
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.gradle.jacoco")

    repositories {
        mavenCentral()
    }

    version = project.properties["releaseVersion"] ?: "LOCAL"
    group = "org.http4k"

    jacoco {
        toolVersion = "0.8.9"
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }

        java {
            sourceCompatibility = VERSION_1_8
            targetCompatibility = VERSION_1_8
        }

        withType<Test> {
            useJUnitPlatform()
            jvmArgs = listOf("--enable-preview")
        }

        named<JacocoReport>("jacocoTestReport") {
            reports {
                html.required.set(true)
                xml.required.set(true)
                csv.required.set(false)
            }
        }

        withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    dependencies {
        testImplementation(Testing.junit.jupiter.api)
        testImplementation(Testing.junit.jupiter.engine)
        testImplementation("com.natpryce:hamkrest:_")
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "idea")

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

    tasks {
        named<Jar>("jar") {
            manifest {
                attributes(mapOf("http4k_version" to archiveVersion))
            }
        }

        val testJar by creating(Jar::class) {
            archiveClassifier.set("test")
            from(project.the<SourceSetContainer>()["test"].output)
        }

        configurations.create("testArtifacts") {
            extendsFrom(configurations["testApi"])
        }
        artifacts {
            add("testArtifacts", testJar)
            archives(sourcesJar)
            archives(javadocJar)
        }
    }

    if (hasAnArtifact(project)) {
        val enableSigning = project.findProperty("sign") == "true"

        apply(plugin = "maven-publish") // required to upload to sonatype

        if (enableSigning) { // when added it expects signing keys to be configured
            apply(plugin = "signing")
            signing {
                val signingKey: String? by project
                val signingPassword: String? by project
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(publishing.publications)
            }
        }

        publishing {
            publications {
                val archivesBaseName = tasks.jar.get().archiveBaseName.get()
                create<MavenPublication>("mavenJava") {
                    artifactId = archivesBaseName
                    pom.withXml {
                        asNode().appendNode("name", archivesBaseName)
                        asNode().appendNode("description", description)
                        asNode().appendNode("url", "https://http4k.org")
                        asNode().appendNode("developers")
                            .appendNode("developer").appendNode("name", "Ivan Sanchez").parent()
                            .appendNode("email", "ivan@http4k.org")
                            .parent().parent()
                            .appendNode("developer").appendNode("name", "David Denton").parent()
                            .appendNode("email", "david@http4k.org")
                        asNode().appendNode("scm")
                            .appendNode("url", "https://github.com/http4k/http4k").parent()
                            .appendNode("connection", "scm:git:git@github.com:http4k/http4k.git").parent()
                            .appendNode("developerConnection", "scm:git:git@github.com:http4k/http4k.git")
                        asNode().appendNode("licenses").appendNode("license")
                            .appendNode("name", "Apache License, Version 2.0").parent()
                            .appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0.html")
                    }
                    from(components["java"])

                    // replace all runtime dependencies with provided
                    pom.withXml {
                        asNode()
                            .childrenCalled("dependencies")
                            .flatMap { it.childrenCalled("dependency") }
                            .flatMap { it.childrenCalled("scope") }
                            .forEach { if (it.text() == "runtime") it.setValue("provided") }
                    }
                    artifact(sourcesJar)
                    artifact(javadocJar)
                }
            }
        }
    }

    sourceSets {
        test {
            kotlin.srcDir("$projectDir/src/examples/kotlin")
        }
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.named<Test>("test").get() })

    sourceDirectories.from(subprojects.flatMap { it.the<SourceSetContainer>()["main"].allSource.srcDirs })
    classDirectories.from(subprojects.map { it.the<SourceSetContainer>()["main"].output })
    executionData.from(subprojects
        .filter { it.name != "http4k-bom" && hasAnArtifact(it) }
        .map {
            it.tasks.named<JacocoReport>("jacocoTestReport").get().executionData
        }
    )

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(file("${buildDir}/reports/jacoco/test/jacocoRootReport.xml"))
    }
}

dependencies {
    subprojects
        .filter { hasAnArtifact(it) }
        .forEach {
            api(project(it.name))
            testImplementation(project(path = it.name, configuration = "testArtifacts"))
        }

    testImplementation("dev.zacsweers.moshix:moshi-metadata-reflect:_")

    testImplementation("software.amazon.awssdk:s3:_") {
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
        exclude(group = "software.amazon.awssdk", module = "apache-client")
    }
    testImplementation("com.expediagroup:graphql-kotlin-schema-generator:_")
    testImplementation("com.amazonaws:aws-lambda-java-events:_")
}

fun hasAnArtifact(it: Project) = !it.name.contains("test-function") && !it.name.contains("integration-test")

sourceSets {
    test {
        kotlin.srcDir("$projectDir/src/docs")
        resources.srcDir("$projectDir/src/docs")
    }
}

tasks.register("listProjects") {
    doLast {
        subprojects
            .filter { hasAnArtifact(it) }
            .forEach { System.err.println(it.name) }
    }
}

fun Node.childrenCalled(wanted: String) = children()
    .filterIsInstance<Node>()
    .filter {
        val name = it.name()
        (name is QName) && name.localPart == wanted
    }

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }
}

val nexusUsername: String? by project
val nexusPassword: String? by project

nexusPublishing {
    repositories {
        sonatype {
            username.set(nexusUsername)
            password.set(nexusPassword)
        }
    }
    transitionCheckOptions {
        maxRetries.set(150)
        delayBetween.set(Duration.ofSeconds(5))
    }
}
