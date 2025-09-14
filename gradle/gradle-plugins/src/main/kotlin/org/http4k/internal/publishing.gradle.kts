package org.http4k.internal

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import groovy.util.Node
import java.net.URI
import groovy.namespace.QName

plugins {
    kotlin("jvm")
    `java-library`
    signing
    `maven-publish`
}

val license: ModuleLicense by project.extra

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

                val ltsPublishingUser: String? by project
                val ltsPublishingPassword: String? by project

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
                val signingKey: String? by project
                val signingPassword: String? by project
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(project.the<PublishingExtension>().publications)
            }
        }

        publishToMavenCentral(automaticRelease = true)

        coordinates(
            when (license) {
                ModuleLicense.Apache2 -> "org.http4k"
                ModuleLicense.Http4kCommercial -> "org.http4k.pro"
            },
            project.name,
            project.properties["releaseVersion"]?.toString() ?: "LOCAL"
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

                val license: ModuleLicense by project.extra

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
