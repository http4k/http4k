/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.http4k.format.Moshi
import java.time.Instant

private data class ArtifactCheckReport(
    val file: String? = null,
    val bundle: String? = null,
    val verification: String
)

private data class ModuleReport(
    val group: String,
    val module: String,
    val version: String,
    val jar_sha256: String,
    val checks: Map<ArtifactType, ArtifactCheckReport>
)

private data class Report(
    val timestamp: String,
    val public_key_fingerprint: String,
    val modules: List<ModuleReport>
)

internal object VerificationReport {

    fun generate(modules: List<ModuleVerification>, publicKeyPem: String?, timestamp: Instant = Instant.now()): String {
        val fingerprint = publicKeyPem
            ?.let { "sha256:" + it.toByteArray().sha256Hex() }
            ?: "unknown"

        val report = Report(
            timestamp = timestamp.toString(),
            public_key_fingerprint = fingerprint,
            modules = modules.map { it.toReport() }
        )

        return Moshi.asFormatString(report)
    }

    private fun ModuleVerification.toReport() = ModuleReport(
        group = group,
        module = module,
        version = version,
        jar_sha256 = jarSha256,
        checks = ArtifactType.entries.associateWith { type ->
            val result = checks[type]
            val name = type.name
            ArtifactCheckReport(
                file = exportedFiles["$name.file"],
                bundle = exportedFiles["$name.bundle"],
                verification = when {
                    result == null -> "not_available"
                    result.passed -> "passed"
                    else -> "failed"
                }
            )
        }
    )
}
