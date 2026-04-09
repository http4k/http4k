/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

@Suppress("EnumEntryName")
enum class ArtifactType(val artifactClassifier: String?, val bundleClassifier: String) {
    jar(null, "jar-sigstore"),
    sbom("cyclonedx", "cyclonedx-sigstore"),
    provenance("provenance", "provenance-sigstore"),
    license("license-report", "license-report-sigstore")
}

data class ModuleVerification(
    val group: String,
    val module: String,
    val version: String,
    val jarSha256: String,
    val checks: Map<ArtifactType, VerificationResult?> = ArtifactType.entries.associateWith { null },
    val exportedFiles: Map<String, String> = emptyMap(),
    val signingKeyFingerprint: KeyFingerprint
) {
    val gav get() = "$group:$module:$version"
    val allPassed get() = checks.values.all { it == null || it.passed }
    val failures get() = checks.filter { it.value?.passed == false }
    val verified get() = checks.count { it.value?.passed == true }

    fun withCheck(type: ArtifactType, result: VerificationResult) =
        copy(checks = checks + (type to result))

    fun withExportedFile(key: String, path: String) =
        copy(exportedFiles = exportedFiles + (key to path))
}
