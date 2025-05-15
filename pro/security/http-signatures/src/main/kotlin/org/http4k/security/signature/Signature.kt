package org.http4k.security.signature

import org.http4k.lens.Header

data class Signature(val label: String, val value: SignatureValue)

val Header.SIGNATURE
    get() = map {
        it.split(",")
            .map(String::trim)
            .map { part ->
                val (labelPart, valuePart) = part.split("=", limit = 2).map(String::trim)
                Signature(labelPart.trim(), valuePart.trim())
            }
    }
        .optional("Signature")
