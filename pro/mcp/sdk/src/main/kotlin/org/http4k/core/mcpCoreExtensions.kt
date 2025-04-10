package org.http4k.core

fun Uri.matchesRfc6570(value: Uri) = try {
    when {
        value.toString().isEmpty() -> false
        else -> {
            val regex = toString()
                .replace(".", "\\.")
                .replace("?", "\\?")
                .replace("*", "\\*")
                .replace("+", "\\+")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("|", "\\|")
                .replace(Regex("\\{[^}]+}"), "([^/]+)")

            Regex("^$regex$").matches(value.toString())
        }
    }
} catch (e: Exception) {
    false
}
