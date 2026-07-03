package org.http4k.routing

fun String.resolvedWithinRoot(): String? =
    if (contains('\u0000') || contains('\\')) {
        null
    } else {
        split('/').fold(emptyList<String>() as List<String>?) { acc, segment ->
            when {
                acc == null -> null
                segment.isEmpty() || segment == "." -> acc
                segment == ".." -> acc.takeIf(List<String>::isNotEmpty)?.dropLast(1)
                else -> acc + segment
            }
        }?.joinToString("/")
    }
