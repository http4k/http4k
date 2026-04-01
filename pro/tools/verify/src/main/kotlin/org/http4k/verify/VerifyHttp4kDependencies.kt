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
import org.gradle.api.tasks.TaskAction
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK

abstract class VerifyHttp4kDependencies : DefaultTask() {

    @get:Input
    abstract val failOnError: Property<Boolean>

    @get:InputFile
    @get:Optional
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

        val artifactsWithBundles = http4kArtifacts.mapNotNull { (id, jarFile) ->
            resolveSigstoreBundle(id)?.let { Triple(id, jarFile, it) }
        }

        if (artifactsWithBundles.isEmpty()) {
            logger.lifecycle("No sigstore bundles found for ${http4kArtifacts.size} http4k artifact(s).")
            logger.lifecycle("Add the http4k Enterprise Repository (maven.http4k.org) to verify artifact signatures.")
            return
        }

        val toVerify = artifactsWithBundles.filter { (id, jarFile, _) ->
            !VerificationCache.isVerified(
                this.project.gradle.gradleUserHomeDir,
                id.group,
                id.module,
                id.version,
                jarFile
            )
        }

        if (toVerify.isEmpty()) {
            val noBundles = http4kArtifacts.size - artifactsWithBundles.size
            logger.lifecycle("All ${artifactsWithBundles.size} http4k artifact(s) previously verified (cached)")
            if (noBundles > 0) {
                logger.lifecycle("  Skipped $noBundles artifact(s) without sigstore bundles (resolved from Maven Central)")
            }
            return
        }

        val results = mutableListOf<VerificationResult>()

        logger.lifecycle("Verifying ${toVerify.size} http4k artifact(s) (${artifactsWithBundles.size - toVerify.size} cached)...")

        for ((id, jarFile, bundleFile) in toVerify) {
            val result = BundleVerifier.verify(jarFile, bundleFile.readText(), loadPublicKey())
            results.add(result)
            if (result.passed) {
                VerificationCache.markVerified(
                    this.project.gradle.gradleUserHomeDir,
                    id.group,
                    id.module,
                    id.version,
                    jarFile
                )
            }
            logger.lifecycle("  ${if (result.passed) "PASS" else "FAIL"}: ${id.group}:${id.module}:${id.version} — ${result.message}")
        }

        val skipped = http4kArtifacts.size - artifactsWithBundles.size
        if (skipped > 0) {
            logger.lifecycle("  Skipped $skipped artifact(s) without sigstore bundles (resolved from Maven Central)")
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
                this.logger.lifecycle("Downloading public key from https://http4k.org/cosign.pub")
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

    private fun resolveSigstoreBundle(id: ModuleComponentIdentifier) =
        try {
            val dep = project.dependencies.create("${id.group}:${id.module}:${id.version}:jar-sigstore@json")
            val config = project.configurations.detachedConfiguration(dep)
            config.resolve().firstOrNull()
        } catch (_: Exception) {
            null
        }
}
