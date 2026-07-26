package org.http4k.internal

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import groovy.namespace.QName
import groovy.util.Node

plugins {
    kotlin("jvm")
    `java-library`
    signing
    `maven-publish`
}

val license = project.extra["license"] as ModuleLicense

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

        publishToMavenCentral(automaticRelease = true)

        coordinates(
            when (license) {
                ModuleLicense.Apache2 -> "org.http4k"
                ModuleLicense.Http4kCommercial -> "org.http4k.pro"
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

                val license = project.extra["license"] as ModuleLicense

                asNode().appendNode("licenses").appendNode("license")
                    .appendNode("name", license.commonName).parent()
                    .appendNode("url", license.url)
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

fun Node.childrenCalled(wanted: String) = children()
    .filterIsInstance<Node>()
    .filter {
        val name = it.name()
        (name is QName) && name.localPart == wanted
    }
