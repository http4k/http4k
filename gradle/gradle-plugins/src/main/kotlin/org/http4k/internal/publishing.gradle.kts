package org.http4k.internal

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import groovy.namespace.QName
import groovy.util.Node
import org.http4k.internal.ModuleLicense.*

plugins {
    kotlin("jvm")
    `java-library`
    signing
    `maven-publish`
}

val license = project.extra["license"] as ModuleLicense

val isEeBranch = rootProject.file("LICENSE-APACHE").exists()

val pomLicense = if (isEeBranch && license == Apache2) Http4kEE else license

val metadata = kotlin.runCatching {
    (project.extensions.getByName("metadata") as? ProjectMetadata.Extension)
}.getOrNull() ?: rootProject.extensions.getByType<ProjectMetadata.Extension>()

apply(plugin = "com.vanniktech.maven.publish")

// workaround so test fixture dependencies don't end up in the published POM
(components["java"] as? AdhocComponentWithVariants)?.apply {
    withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
    withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
}

configure<MavenPublishBaseExtension> {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = "http4kLts"
                url = rootProject.layout.buildDirectory.dir("lts-staging").get().asFile.toURI()
            }
        }

        val enableSigning = project.findProperty("sign") == "true"

        if (enableSigning) {
            apply(plugin = "signing")
            signing {
                val signingKey = project.findProperty("signingKey") as String?
                val signingPassword = project.findProperty("signingPassword") as String?
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(project.the<PublishingExtension>().publications)
            }

            project.afterEvaluate {
                tasks.withType<PublishToMavenRepository>().configureEach {
                    dependsOn(tasks.withType<Sign>())
                }
            }
        }

        publishToMavenCentral(automaticRelease = false)

        coordinates(
            when (license) {
                Apache2, Http4kEE -> "org.http4k"
                Http4kCommercial -> "org.http4k.pro"
            },
            project.name,
            project.findProperty("releaseVersion")?.toString() ?: "LOCAL"
        )

        pom {
            withXml {
                asNode().appendNode("name", project.name)
                asNode().appendNode("description", project.description)
                asNode().appendNode("url", "https://http4k.org")
                asNode().appendNode("developers").apply {
                    metadata.developers
                        .forEach { (name, email) ->
                            appendNode("developer").appendNode("name", name).parent()
                                .appendNode("email", email)
                        }
                }
                asNode().appendNode("scm")
                    .appendNode("url", "https://github.com/http4k/${rootProject.name}").parent()
                    .appendNode("connection", "scm:git:git@github.com:http4k/${rootProject.name}.git").parent()
                    .appendNode("developerConnection", "scm:git:git@github.com:http4k/${rootProject.name}.git")

                asNode().appendNode("licenses").appendNode("license").apply {
                    appendNode("name", pomLicense.commonName)
                    appendNode("url", pomLicense.url)
                    pomLicense.comments?.let { appendNode("comments", it) }
                }
            }

            // replace all runtime dependencies with provided
            withXml {
                asNode()
                    .childrenCalled("dependencies")
                    .flatMap { it.childrenCalled("dependency") }
                    .flatMap { it.childrenCalled("scope") }
                    .forEach { if (it.text() == "runtime") it.setValue("provided") }
            }
        }
    }

}

val releaseVersion = project.findProperty("releaseVersion")?.toString()
val isPrivateRelease = releaseVersion != null && (releaseVersion.endsWith("-ee") || releaseVersion.endsWith("-lts"))

if (isEeBranch && releaseVersion != null && !releaseVersion.endsWith("-ee")) {
    throw GradleException("ee branch release version must end in -ee, got $releaseVersion")
}

// the repository is only assigned after task creation, so check it when the task runs
tasks.withType<PublishToMavenRepository>().configureEach {
    doFirst {
        if (repository?.name == "mavenCentral" && (isEeBranch || isPrivateRelease)) {
            throw GradleException("Refusing to publish $releaseVersion to Maven Central: EE/LTS artefacts are private-repo only")
        }
    }
}

fun Node.childrenCalled(wanted: String) = children()
    .filterIsInstance<Node>()
    .filter {
        val name = it.name()
        (name is QName) && name.localPart == wanted
    }
