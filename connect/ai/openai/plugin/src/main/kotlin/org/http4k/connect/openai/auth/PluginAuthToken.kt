package org.http4k.connect.openai.auth

import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.contract.security.BearerAuthSecurity
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.lens.RequestContextLens
import org.http4k.security.AccessToken

/**
 * Represents the token type which can be used for User and Service authed plugins.
 */
interface PluginAuthToken {
    val securityFilter: Filter
    val type: String

    class Basic<Principal> private constructor(
        key: RequestContextLens<Principal>?,
        check: (Credentials) -> Principal?,
        realm: String,
    ) : PluginAuthToken {

        /**
         * Use when you need access to the stored principal
         */
        constructor(
            realm: String,
            key: RequestContextLens<Principal>,
            check: (Credentials) -> Principal?
        ) : this(key, check, realm)

        override val type = "basic"

        override val securityFilter = when (key) {
            null -> BasicAuthSecurity(realm) { check(it) != null }
            else -> BasicAuthSecurity(realm, key, check)
        }.filter

        companion object {
            /**
             * Use when you don't need access to the stored principal
             */
            operator fun invoke(realm: String, check: (Credentials) -> Boolean) =
                Basic(null, { if (check(it)) Unit else null }, realm)
        }
    }

    /**
     * Use when you need access to the stored principal
     */
    class Bearer<Principal> private constructor(
        check: (AccessToken) -> Principal?,
        key: RequestContextLens<Principal>?
    ) : PluginAuthToken {

        /**
         * Use when you need access to the stored principal
         */
        constructor(
            key: RequestContextLens<Principal>,
            check: (AccessToken) -> Principal?
        ) : this(check, key as RequestContextLens<Principal>?)

        override val type = "bearer"

        override val securityFilter = when (key) {
            null -> BearerAuthSecurity({ check(AccessToken(it)) != null })
            else -> BearerAuthSecurity(key, { check(AccessToken(it)) })
        }.filter

        companion object {
            /**
             * Use when you don't need access to the stored principal
             */
            operator fun invoke(check: (AccessToken) -> Boolean) = Bearer(
                { if (check(it)) Unit else null }, null
            )
        }
    }
}
