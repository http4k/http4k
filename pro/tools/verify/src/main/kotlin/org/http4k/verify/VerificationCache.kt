/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import java.io.File
import java.security.MessageDigest

fun ByteArray.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }

fun File.sha256Hex(): String = readBytes().sha256Hex()

class VerificationCache(gradleUserHome: File = File(System.getProperty("user.home"), ".gradle")) {
    private val home = File(gradleUserHome, "caches/http4k-verify").apply { mkdirs() }
    private val cacheFile = File(home, "verified.txt")

    private fun cacheKey(label: String, artifact: File) = "$label:${artifact.sha256Hex()}"

    fun isVerified(label: String, artifact: File): Boolean {
        val key = cacheKey(label, artifact)
        return cacheFile.exists() && cacheFile.readLines().contains(key)
    }

    fun markVerified(label: String, artifact: File) {
        val key = cacheKey(label, artifact)
        if (!cacheFile.exists() || !cacheFile.readLines().contains(key)) {
            cacheFile.appendText("$key\n")
        }
    }

    fun clear() = cacheFile.exists().also { if (it) cacheFile.delete() }
}
