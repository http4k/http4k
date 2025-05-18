package org.http4k.lens

import org.http4k.security.signature.Signature

val Header.SIGNATURE
    get() = map({
        it.split(",")
            .map(String::trim)
            .map { part ->
                val (labelPart, valuePart) = part.split("=", limit = 2).map(String::trim)
                Signature(labelPart.trim(), valuePart.trim())
            }
    }, { it.joinToString(",") { "${it.label}=${it.value}" } })
        .optional("Signature")
