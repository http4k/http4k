/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.http4k.format.Moshi
import org.http4k.verify.VerificationStatus.failed
import org.http4k.verify.VerificationStatus.not_available
import org.http4k.verify.VerificationStatus.passed
import java.time.Instant

@Suppress("EnumEntryName")
enum class VerificationStatus { passed, failed, not_available }

data class ArtifactCheckReport(
    val file: String? = null,
    val bundle: String? = null,
    val verification: VerificationStatus
)

data class ModuleReport(
    val group: String,
    val module: String,
    val version: String,
    val jar_sha256: String,
    val signing_key_fingerprint: String? = null,
    val checks: Map<ArtifactType, ArtifactCheckReport>
)

data class Report(
    val timestamp: String,
    val modules: List<ModuleReport>
)

object VerificationReport {

    fun generate(modules: List<ModuleVerification>, timestamp: Instant = Instant.now()): String {
        val report = Report(
            timestamp = timestamp.toString(),
            modules = modules.map { it.toReport() }
        )

        return Moshi.asFormatString(report)
    }

    private fun ModuleVerification.toReport() = ModuleReport(
        group = group,
        module = module,
        version = version,
        jar_sha256 = jarSha256,
        signing_key_fingerprint = signingKeyFingerprint.value,
        checks = ArtifactType.entries.associateWith { type ->
            val result = checks[type]
            val name = type.name
            ArtifactCheckReport(
                file = exportedFiles["$name.file"],
                bundle = exportedFiles["$name.bundle"],
                verification = when {
                    result == null -> not_available
                    result.passed -> passed
                    else -> failed
                }
            )
        }
    )
}
