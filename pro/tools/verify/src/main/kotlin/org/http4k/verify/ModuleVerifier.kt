/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import java.io.File

class ModuleVerifier(
    private val cache: VerificationCache,
    private val bundleVerifier: BundleVerifier,
    private val resolveClassified: (ModuleComponentIdentifier, String) -> File?
) {
    fun verify(id: ModuleComponentIdentifier, jarFile: File, outputDir: File): ModuleVerification {
        val jarSha256 = jarFile.sha256Hex()

        val moduleDir = File(outputDir, "${id.group}/${id.module}/${id.version}").apply { mkdirs() }
        val baseName = "${id.module}-${id.version}"
        val shaPath = "${id.group}/${id.module}/${id.version}/$baseName.jar.sha256"
        File(moduleDir, "$baseName.jar.sha256").writeText(jarSha256)

        val seed = ModuleVerification(
            group = id.group,
            module = id.module,
            version = id.version,
            jarSha256 = jarSha256,
            exportedFiles = mapOf("jar.sha256" to shaPath)
        )

        return ArtifactType.entries.fold(seed) { module, type ->
            val artifactToVerify = type.artifactClassifier?.let { resolveClassified(id, it) } ?: jarFile
            val bundle = resolveClassified(id, type.bundleClassifier) ?: return@fold module

            val exportPath = { classifier: String -> "${id.group}/${id.module}/${id.version}/$baseName-$classifier.json" }

            val withExports = module.let { m ->
                val withArtifact = type.artifactClassifier?.let { classifier ->
                    artifactToVerify.copyTo(File(moduleDir, "$baseName-$classifier.json"), overwrite = true)
                    m.withExportedFile("${type.name}.file", exportPath(classifier))
                } ?: m

                bundle.copyTo(File(moduleDir, "$baseName-${type.bundleClassifier}.json"), overwrite = true)
                withArtifact.withExportedFile("${type.name}.bundle", exportPath(type.bundleClassifier))
            }

            val cacheKey = "${withExports.gav}:${type.name}"
            val result = when {
                cache.isVerified(cacheKey, artifactToVerify) ->
                    VerificationResult(artifactToVerify.name, true, "cached")

                else -> bundleVerifier.verify(artifactToVerify, bundle.readText()).also {
                    if (it.passed) cache.markVerified(cacheKey, artifactToVerify)
                }
            }

            withExports.withCheck(type, result)
        }
    }
}
