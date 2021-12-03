import groovy.util.Node
import groovy.xml.QName
import org.gradle.api.JavaVersion.VERSION_1_8
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    idea
    jacoco
    signing
    publishing
    `maven-publish`
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
        classpath("com.github.jengelman.gradle.plugins:shadow:_")
        classpath("io.codearte.nexus-staging:io.codearte.nexus-staging.gradle.plugin:_")
    }
}

apply(plugin = "io.codearte.nexus-staging")

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.gradle.jacoco")

    repositories {
        mavenCentral()
    }

    version = project.getProperties()["releaseVersion"] ?: "LOCAL"
    group = "org.http4k"

    jacoco {
        toolVersion = "0.8.7"
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
        }

        named<JacocoReport>("jacocoTestReport") {
            reports {
                html.isEnabled = true
                xml.isEnabled = true
            }
        }

        withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:_")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:_")
        testImplementation("org.jetbrains.kotlin:kotlin-reflect:_")
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
        apply(plugin = "maven") // required to upload to sonatype

        if (enableSigning) { // when added it expects signing keys to be configured
            apply(plugin = "signing")
            signing {
                val signingKey: String? by project
                val signingPassword: String? by project
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(publishing.publications)
            }
        }

        val nexusUsername: String? by project
        val nexusPassword: String? by project

        publishing {
            repositories {
                maven {
                    name = "SonatypeStaging"
                    setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = nexusUsername
                        password = nexusPassword
                    }
                }
                maven {
                    name = "SonatypeSnapshot"
                    setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
                    credentials {
                        username = nexusUsername
                        password = nexusPassword
                    }
                }
            }
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
                            .appendNode("url", "git@github.com:http4k/$archivesBaseName.git").parent()
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
        named("test") {
            withConvention(KotlinSourceSet::class) {
                kotlin.srcDir("$projectDir/src/examples/kotlin")
            }
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
        html.isEnabled = true
        xml.isEnabled = true
        csv.isEnabled = false
        xml.destination = file("${buildDir}/reports/jacoco/test/jacocoRootReport.xml")
    }
}

dependencies {
    subprojects
        .filter { hasAnArtifact(it) }
        .forEach {
            api(project(it.name))
            testImplementation(project(path = it.name, configuration = "testArtifacts"))
        }

    testImplementation("software.amazon.awssdk:s3:_") {
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
        exclude(group = "software.amazon.awssdk", module = "apache-client")
    }
    testImplementation("com.expediagroup:graphql-kotlin-schema-generator:_")
    testImplementation("com.amazonaws:aws-lambda-java-events:_")
}

fun hasAnArtifact(it: Project) = !it.name.contains("test-function") && !it.name.contains("integration-test")

sourceSets {
    named("test") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("$projectDir/src/docs")
            resources.srcDir("$projectDir/src/docs")
        }
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
        freeCompilerArgs = freeCompilerArgs + listOf("-Xjvm-default=all")
    }
}
