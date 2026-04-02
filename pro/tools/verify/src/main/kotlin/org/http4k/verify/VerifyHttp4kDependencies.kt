/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import java.io.File

private data class VerifiableArtifact(
    val label: String,
    val artifact: File,
    val bundle: File
)

private val CLASSIFIED_ARTIFACTS = listOf(
    "cyclonedx-sigstore" to "cyclonedx",
    "provenance-sigstore" to "provenance"
)

@DisableCachingByDefault(because = "Verification uses its own caching mechanism")
abstract class VerifyHttp4kDependencies : DefaultTask() {

    @get:Input
    abstract val failOnError: Property<Boolean>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(NONE)
    abstract val publicKeyFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val runtimeClasspath = project.configurations.findByName("runtimeClasspath")
            ?: run {
                logger.warn("No runtimeClasspath configuration found, skipping verification")
                return
            }

        val http4kArtifacts = resolveHttp4kArtifacts(runtimeClasspath)

        if (http4kArtifacts.isEmpty()) {
            logger.lifecycle("No http4k artifacts found to verify")
            return
        }

        val allVerifiable = http4kArtifacts.flatMap { (id, jarFile) -> resolveVerifiableArtifacts(id, jarFile) }

        if (allVerifiable.isEmpty()) {
            logger.lifecycle("No sigstore bundles found for ${http4kArtifacts.size} http4k artifact(s).")
            logger.lifecycle("Add the http4k Enterprise Repository (maven.http4k.org) to verify artifact signatures.")
            return
        }

        val gradleHome = project.gradle.gradleUserHomeDir
        val toVerify = allVerifiable.filter { (label, artifact, _) ->
            !VerificationCache.isVerified(gradleHome, label, artifact)
        }

        if (toVerify.isEmpty()) {
            logger.lifecycle("All ${allVerifiable.size} http4k artifact signature(s) previously verified (cached)")
            return
        }

        val publicKey = loadPublicKey()
        val results = mutableListOf<VerificationResult>()
        val cached = allVerifiable.size - toVerify.size

        logger.lifecycle("Verifying ${toVerify.size} http4k artifact signature(s)${if (cached > 0) " ($cached cached)" else ""}...")

        for ((label, artifact, bundle) in toVerify) {
            val result = BundleVerifier.verify(artifact, bundle.readText(), publicKey)
            results.add(result)
            if (result.passed) {
                VerificationCache.markVerified(gradleHome, label, artifact)
            }
            logger.lifecycle("  ${if (result.passed) "PASS" else "FAIL"}: $label — ${result.message}")
        }

        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        logger.lifecycle("Verification complete: $passed passed, $failed failed")

        if (failed > 0 && failOnError.get()) {
            throw GradleException("http4k artifact verification failed for $failed artifact(s)")
        }
    }

    private fun loadPublicKey() = BundleVerifier.loadPublicKey(
        when {
            publicKeyFile.isPresent -> publicKeyFile.get().asFile.readText()
            else -> {
                logger.lifecycle("Downloading public key from https://http4k.org/cosign.pub")
                val response = JavaHttpClient()(Request(GET, "https://http4k.org/cosign.pub"))
                if (response.status != OK) throw GradleException("Failed to download public key: ${response.status}")
                response.bodyString()
            }
        }
    )

    private fun resolveHttp4kArtifacts(configuration: Configuration) =
        configuration.resolvedConfiguration.resolvedArtifacts
            .filter { artifact ->
                val id = artifact.id.componentIdentifier
                id is ModuleComponentIdentifier &&
                    (id.group == "org.http4k" || id.group.startsWith("org.http4k."))
            }
            .map { artifact ->
                artifact.id.componentIdentifier as ModuleComponentIdentifier to artifact.file
            }

    private fun resolveVerifiableArtifacts(id: ModuleComponentIdentifier, jarFile: File): List<VerifiableArtifact> {
        val results = mutableListOf<VerifiableArtifact>()

        resolveClassified(id, "jar-sigstore")?.let { bundle ->
            results += VerifiableArtifact(
                label = "${id.group}:${id.module}:${id.version}:jar",
                artifact = jarFile,
                bundle = bundle
            )
        }

        CLASSIFIED_ARTIFACTS.forEach { (bundleClassifier, artifactClassifier) ->
            val bundle = resolveClassified(id, bundleClassifier) ?: return@forEach
            val artifact = resolveClassified(id, artifactClassifier) ?: return@forEach
            results += VerifiableArtifact(
                label = "${id.group}:${id.module}:${id.version}:$artifactClassifier",
                artifact = artifact,
                bundle = bundle
            )
        }

        return results
    }

    private fun resolveClassified(id: ModuleComponentIdentifier, classifier: String): File? =
        try {
            val dep = project.dependencies.create("${id.group}:${id.module}:${id.version}:$classifier@json")
            val config = project.configurations.detachedConfiguration(dep)
            config.resolve().firstOrNull()
        } catch (_: Exception) {
            null
        }
}
