package org.http4k.internal

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import groovy.namespace.QName
import groovy.util.Node
import java.net.URI

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
                name = "http4k"
                url = URI("s3://http4k-maven")

                val ltsPublishingUser = project.findProperty("ltsPublishingUser") as String?
                val ltsPublishingPassword = project.findProperty("ltsPublishingPassword") as String?

                credentials(AwsCredentials::class.java) {
                    accessKey = ltsPublishingUser
                    secretKey = ltsPublishingPassword
                }
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

        if (project.findProperty("includeProvenance") == "true") {
            val version = project.findProperty("releaseVersion")?.toString() ?: "LOCAL"
            val buildDir = project.layout.buildDirectory.get().asFile

            project.afterEvaluate {
                publications.withType<MavenPublication>().matching { it.name == "maven" || it.name == "pluginMaven" }.configureEach {
                    artifact(File(buildDir, "reports/${project.name}-sbom.json")) {
                        classifier = "cyclonedx"
                        extension = "json"
                    }
                    artifact(File(buildDir, "reports/${project.name}-sbom.json.sigstore.json")) {
                        classifier = "cyclonedx-sigstore"
                        extension = "json"
                    }
                    val libsDir = File(buildDir, "libs")
                    // One signature per published jar (main, sources, javadoc). The jars are
                    // versioned and signed by bin/sign-and-attest.sh; the build step must build
                    // every javadoc variant before signing (see #1575).
                    listOf("" to "jar", "-sources" to "sources", "-javadoc" to "javadoc").forEach { (suffix, cls) ->
                        artifact(File(libsDir, "${project.name}-${version}$suffix.jar.sigstore.json")) {
                            classifier = "$cls-sigstore"
                            extension = "json"
                        }
                    }
                    // test fixtures only exist for some modules
                    File(libsDir, "${project.name}-${version}-test-fixtures-sources.jar.sigstore.json")
                        .takeIf { it.exists() }
                        ?.let { sig ->
                            artifact(sig) {
                                classifier = "test-fixtures-sources-sigstore"
                                extension = "json"
                            }
                        }
                    artifact(
                        File(
                            rootProject.layout.buildDirectory.get().asFile,
                            "provenance/${project.name}-${version}.provenance.json"
                        )
                    ) {
                        classifier = "provenance"
                        extension = "json"
                    }
                    artifact(
                        File(
                            rootProject.layout.buildDirectory.get().asFile,
                            "provenance/${project.name}-${version}.provenance.json.sigstore.json"
                        )
                    ) {
                        classifier = "provenance-sigstore"
                        extension = "json"
                    }
                    artifact(File(buildDir, "reports/${project.name}-license-report.json")) {
                        classifier = "license-report"
                        extension = "json"
                    }
                    artifact(File(buildDir, "reports/${project.name}-license-report.json.sigstore.json")) {
                        classifier = "license-report-sigstore"
                        extension = "json"
                    }
                    artifact(File(buildDir, "publications/$name/pom-default.xml.sigstore.json")) {
                        classifier = "pom-sigstore"
                        extension = "json"
                    }
                }
            }
        }

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
