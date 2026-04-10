/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.format.Moshi
import java.io.File

@DisableCachingByDefault(because = "Verification uses its own caching mechanism")
abstract class VerifyHttp4kDependencies : DefaultTask() {

    @get:Input
    abstract val failOnError: Property<Boolean>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(NONE)
    abstract val publicKeyFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val keyListUrl: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun verify() {
        val classpaths = listOf("runtimeClasspath", "testRuntimeClasspath")
            .mapNotNull { project.configurations.findByName(it) }

        if (classpaths.isEmpty()) {
            logger.warn("No classpath configurations found, skipping verification")
            return
        }

        val http4kArtifacts = classpaths
            .flatMap { resolveHttp4kArtifacts(it) }
            .distinctBy { (id, _) -> "${id.group}:${id.module}:${id.version}" }

        if (http4kArtifacts.isEmpty()) {
            logger.lifecycle("No http4k artifacts found to verify")
            return
        }

        val outputDir = outputDirectory.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val singleKeyMode = publicKeyFile.isPresent
        val client = ClientFilters.FollowRedirects().then(JavaHttpClient())

        val resolveBundleVerifier: (KeyFingerprint) -> BundleVerifier
        val publicKeyPem: String?
        val keyList: CosignKeyList?

        if (singleKeyMode) {
            val (publicKey, pemText) = PublicKeyLoader(
                publicKeyFile = publicKeyFile.orNull?.asFile,
                log = { logger.lifecycle(it) },
                client = client
            ).load()
            publicKeyPem = pemText
            keyList = null
            resolveBundleVerifier = { BundleVerifier(publicKey) }
        } else {
            keyList = KeyListLoader(
                url = Uri.of(keyListUrl.get()),
                log = { logger.lifecycle(it) },
                client = client,
                cacheDir = File(project.gradle.gradleUserHomeDir, "caches/http4k-verify")
            ).load()
            publicKeyPem = null
            val keyResolver = KeyResolver(keyList)
            resolveBundleVerifier = { fingerprint -> BundleVerifier(keyResolver.resolve(fingerprint).key) }
        }

        val verifier = ModuleVerifier(
            cache = VerificationCache(project.gradle.gradleUserHomeDir),
            resolveBundleVerifier = resolveBundleVerifier,
            resolveClassified = ::resolveClassified
        )

        val modules = http4kArtifacts.map { (id, jarFile) ->
            verifier.verify(id, jarFile, outputDir)
        }

        val anyBundleFound = modules.any { m -> m.checks.values.any { it != null } }

        if (!anyBundleFound) {
            logger.lifecycle("No sigstore bundles found for ${http4kArtifacts.size} http4k artifact(s).")
            logger.lifecycle("Add the http4k Enterprise Repository (maven.http4k.org) to verify artifact signatures.")
            return
        }

        if (publicKeyPem != null) {
            File(outputDir, "cosign.pub").writeText(publicKeyPem)
        }
        if (keyList != null) {
            File(outputDir, "cosign-keys.json").writeText(Moshi.asFormatString(keyList))
        }
        File(outputDir, "verification-report.json")
            .writeText(VerificationReport.generate(modules))

        logResults(modules)

        val totalFailed = modules.sumOf { it.failures.size }
        if (totalFailed > 0 && failOnError.get()) {
            throw GradleException("http4k artifact verification failed for $totalFailed signature(s)")
        }
    }

    private fun logResults(modules: List<ModuleVerification>) {
        val newlyVerified = modules.filter { m -> m.checks.values.any { it != null && it.message != "cached" } }

        if (newlyVerified.isNotEmpty()) {
            logger.lifecycle("Verifying ${newlyVerified.size} http4k module(s)...")
            val pad = (newlyVerified.maxOf { it.gav.length } + 4).coerceAtLeast(50)

            newlyVerified.forEach { module ->
                val checks = module.checks.entries.joinToString("   ") { (name, result) ->
                    when {
                        result == null -> "$name -"
                        result.passed -> "$name \u2713"
                        else -> "$name \u2717"
                    }
                }
                logger.lifecycle("  ${module.gav.padEnd(pad)} $checks")

                module.failures.forEach { (name, result) ->
                    logger.lifecycle("    FAIL: $name — ${result!!.message}")
                }
            }

            val totalVerified = modules.sumOf { it.verified }
            val totalFailed = modules.sumOf { it.failures.size }
            logger.lifecycle("Verified: ${newlyVerified.size} modules, $totalVerified signatures" +
                if (totalFailed > 0) ", $totalFailed failed" else "")
        }

        logger.lifecycle("Verification artifacts exported to ${outputDirectory.get().asFile.relativeTo(project.projectDir)}")
    }

    private fun resolveHttp4kArtifacts(configuration: Configuration) =
        configuration.resolvedConfiguration.resolvedArtifacts
            .filter { artifact ->
                val id = artifact.id.componentIdentifier
                id is ModuleComponentIdentifier &&
                    (id.group == "org.http4k" || id.group.startsWith("org.http4k."))
            }
            .map { it.id.componentIdentifier as ModuleComponentIdentifier to it.file }

    private fun resolveClassified(id: ModuleComponentIdentifier, classifier: String): File? =
        try {
            val dep = project.dependencies.create("${id.group}:${id.module}:${id.version}:$classifier@json")
            val config = project.configurations.detachedConfiguration(dep)
            config.resolve().firstOrNull()
        } catch (_: Exception) {
            null
        }
}
