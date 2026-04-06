/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.http4k.format.Moshi
import java.time.Instant

internal object VerificationReport {

    fun generate(modules: List<ModuleVerification>, publicKeyPem: String?, timestamp: Instant = Instant.now()): String {
        val fingerprint = publicKeyPem
            ?.let { "sha256:" + it.toByteArray().sha256Hex() }
            ?: "unknown"

        val modulesArray = modules.map { module ->
            val checksFields = module.checks.entries.map { (type, result) ->
                val verification = when {
                    result == null -> "not_available"
                    result.passed -> "passed"
                    else -> "failed"
                }
                val name = type.name
                val fields = listOfNotNull(
                    module.exportedFiles["$name.file"]?.let { "file" to Moshi.string(it) },
                    module.exportedFiles["$name.bundle"]?.let { "bundle" to Moshi.string(it) },
                    "verification" to Moshi.string(verification)
                )
                name to Moshi.obj(fields)
            }

            Moshi.obj(
                listOf(
                    "group" to Moshi.string(module.group),
                    "module" to Moshi.string(module.module),
                    "version" to Moshi.string(module.version),
                    "jar_sha256" to Moshi.string(module.jarSha256)
                ) + checksFields
            )
        }

        return Moshi.pretty(
            Moshi.obj(
                "timestamp" to Moshi.string(timestamp.toString()),
                "public_key_fingerprint" to Moshi.string(fingerprint),
                "modules" to Moshi.array(modulesArray)
            )
        )
    }
}
