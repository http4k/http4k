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
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import java.io.File

private data class ModuleVerification(
    val gav: String,
    val checks: MutableMap<String, VerificationResult?> = mutableMapOf(
        "jar" to null, "sbom" to null, "provenance" to null, "license" to null
    )
) {
    val allPassed get() = checks.values.all { it == null || it.passed }
    val failures get() = checks.filter { it.value?.passed == false }
    val verified get() = checks.count { it.value?.passed == true }
}

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

        val gradleHome = project.gradle.gradleUserHomeDir
        val modules = mutableListOf<ModuleVerification>()
        var totalVerified = 0
        var totalFailed = 0
        var anyBundleFound = false

        for ((id, jarFile) in http4kArtifacts) {
            val gav = "${id.group}:${id.module}:${id.version}"
            val module = ModuleVerification(gav)

            val jarBundle = resolveClassified(id, "jar-sigstore")
            if (jarBundle != null) {
                anyBundleFound = true
                val cacheKey = "$gav:jar"
                if (VerificationCache.isVerified(gradleHome, cacheKey, jarFile)) {
                    module.checks["jar"] = VerificationResult(jarFile.name, true, "cached")
                } else {
                    val publicKey = loadPublicKey()
                    val result = BundleVerifier.verify(jarFile, jarBundle.readText(), publicKey)
                    module.checks["jar"] = result
                    if (result.passed) VerificationCache.markVerified(gradleHome, cacheKey, jarFile)
                }
            }

            val sbomArtifact = resolveClassified(id, "cyclonedx")
            val sbomBundle = resolveClassified(id, "cyclonedx-sigstore")
            if (sbomArtifact != null && sbomBundle != null) {
                val cacheKey = "$gav:sbom"
                if (VerificationCache.isVerified(gradleHome, cacheKey, sbomArtifact)) {
                    module.checks["sbom"] = VerificationResult(sbomArtifact.name, true, "cached")
                } else {
                    val publicKey = loadPublicKey()
                    val result = BundleVerifier.verify(sbomArtifact, sbomBundle.readText(), publicKey)
                    module.checks["sbom"] = result
                    if (result.passed) VerificationCache.markVerified(gradleHome, cacheKey, sbomArtifact)
                }
            }

            val provArtifact = resolveClassified(id, "provenance")
            val provBundle = resolveClassified(id, "provenance-sigstore")
            if (provArtifact != null && provBundle != null) {
                val cacheKey = "$gav:provenance"
                if (VerificationCache.isVerified(gradleHome, cacheKey, provArtifact)) {
                    module.checks["provenance"] = VerificationResult(provArtifact.name, true, "cached")
                } else {
                    val publicKey = loadPublicKey()
                    val result = BundleVerifier.verify(provArtifact, provBundle.readText(), publicKey)
                    module.checks["provenance"] = result
                    if (result.passed) VerificationCache.markVerified(gradleHome, cacheKey, provArtifact)
                }
            }

            val licenseArtifact = resolveClassified(id, "license-report")
            val licenseBundle = resolveClassified(id, "license-report-sigstore")
            if (licenseArtifact != null && licenseBundle != null) {
                val cacheKey = "$gav:license"
                if (VerificationCache.isVerified(gradleHome, cacheKey, licenseArtifact)) {
                    module.checks["license"] = VerificationResult(licenseArtifact.name, true, "cached")
                } else {
                    val publicKey = loadPublicKey()
                    val result = BundleVerifier.verify(licenseArtifact, licenseBundle.readText(), publicKey)
                    module.checks["license"] = result
                    if (result.passed) VerificationCache.markVerified(gradleHome, cacheKey, licenseArtifact)
                }
            }

            totalVerified += module.verified
            totalFailed += module.failures.size
            modules += module
        }

        if (!anyBundleFound) {
            logger.lifecycle("No sigstore bundles found for ${http4kArtifacts.size} http4k artifact(s).")
            logger.lifecycle("Add the http4k Enterprise Repository (maven.http4k.org) to verify artifact signatures.")
            return
        }

        val newlyVerified = modules.filter { m -> m.checks.values.any { it?.message != "cached" && it != null } }

        if (newlyVerified.isEmpty() && totalFailed == 0) return

        if (newlyVerified.isNotEmpty()) {
            logger.lifecycle("Verifying ${newlyVerified.size} http4k module(s)...")
            val maxGavLen = newlyVerified.maxOf { it.gav.length }
            val pad = (maxGavLen + 4).coerceAtLeast(50)

            for (module in newlyVerified) {
                val checks = module.checks.entries.joinToString("   ") { (name, result) ->
                    when {
                        result == null -> "$name -"
                        result.passed -> "$name \u2713"
                        else -> "$name \u2717"
                    }
                }
                logger.lifecycle("  ${module.gav.padEnd(pad)} $checks")

                for ((name, result) in module.failures) {
                    logger.lifecycle("    FAIL: $name — ${result!!.message}")
                }
            }

            logger.lifecycle("Verified: ${newlyVerified.size} modules, $totalVerified signatures" +
                if (totalFailed > 0) ", $totalFailed failed" else "")
        }

        if (totalFailed > 0 && failOnError.get()) {
            throw GradleException("http4k artifact verification failed for $totalFailed signature(s)")
        }
    }

    private var cachedPublicKey: java.security.PublicKey? = null
    private val client = ClientFilters.FollowRedirects()
        .then(JavaHttpClient())

    private fun loadPublicKey(): java.security.PublicKey {
        cachedPublicKey?.let { return it }
        val key = BundleVerifier.loadPublicKey(
            when {
                publicKeyFile.isPresent -> publicKeyFile.get().asFile.readText()
                else -> {
                    logger.lifecycle("Downloading public key from https://www.http4k.org/cosign.pub")
                    val response = client(Request(GET, "https://www.http4k.org/cosign.pub"))
                    if (response.status != OK) throw GradleException("Failed to download public key: ${response.status}")
                    response.bodyString()
                }
            }
        )
        cachedPublicKey = key
        return key
    }

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

    private fun resolveClassified(id: ModuleComponentIdentifier, classifier: String): File? =
        try {
            val dep = project.dependencies.create("${id.group}:${id.module}:${id.version}:$classifier@json")
            val config = project.configurations.detachedConfiguration(dep)
            config.resolve().firstOrNull()
        } catch (_: Exception) {
            null
        }
}
