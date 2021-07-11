package org.http4k.filter.auth.digest

/**
 * Quality-of-Protection describes the security level of the Authorization challenge
 */
enum class Qop(val value: String) {
    AuthInt("auth-int"),
    Auth("auth");

    companion object {
        fun parse(value: String) = values()
            .firstOrNull { it.value == value }
    }
}
