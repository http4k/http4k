package org.http4k.contract.openapi.v3

import org.http4k.contract.openapi.Render
import org.http4k.contract.openapi.RenderModes
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.contract.openapi.rendererFor
import org.http4k.security.ApiKeySecurity
import org.http4k.security.AuthCodeOAuthSecurity
import org.http4k.security.BasicAuthSecurity
import org.http4k.security.BearerAuthSecurity
import org.http4k.security.ClientCredentialsOAuthSecurity
import org.http4k.security.ImplicitOAuthSecurity
import org.http4k.security.OpenIdConnectSecurity
import org.http4k.security.UserCredentialsOAuthSecurity

/**
 * Compose the supported Security models
 */
val OpenApi3SecurityRenderer: SecurityRenderer = SecurityRenderer(
    ApiKeySecurity.renderer,
    AuthCodeOAuthSecurity.renderer,
    BasicAuthSecurity.renderer,
    BearerAuthSecurity.renderer,
    ImplicitOAuthSecurity.renderer,
    UserCredentialsOAuthSecurity.renderer,
    OpenIdConnectSecurity.renderer,
    ClientCredentialsOAuthSecurity.renderer
)

val ApiKeySecurity.Companion.renderer
    get() = rendererFor<ApiKeySecurity<*>> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(
                    it.name to obj(
                        "type" to string("apiKey"),
                        "in" to string(it.param.meta.location),
                        "name" to string(it.param.meta.name)
                    )
                )
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(emptyList())) }
        }
    }

val AuthCodeOAuthSecurity.Companion.renderer
    get() = rendererFor<AuthCodeOAuthSecurity> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(it.name to obj(
                    "type" to string("oauth2"),
                    "flows" to obj("authorizationCode" to
                        obj(
                            listOfNotNull(
                                "authorizationUrl" to string(it.authorizationUrl.toString()),
                                it.refreshUrl?.let { "refreshUrl" to string(it.toString()) },
                                "tokenUrl" to string(it.tokenUrl.toString()),
                                "scopes" to obj(it.scopes.map { it.name to string(it.description) }
                                )
                            ) + it.extraFields.map { it.key to string(it.value) }
                        )
                    )
                ))
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(it.scopes.map { string(it.name) })) }
        }
    }

val BasicAuthSecurity.Companion.renderer
    get() = rendererFor<BasicAuthSecurity> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(
                    it.name to obj(
                        "scheme" to string("basic"),
                        "type" to string("http")
                    )
                )
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(emptyList())) }
        }
    }

val BearerAuthSecurity.Companion.renderer
    get() = rendererFor<BearerAuthSecurity> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(
                    it.name to obj(
                        "scheme" to string("bearer"),
                        "type" to string("http")
                    )
                )
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(emptyList())) }
        }
    }

val ImplicitOAuthSecurity.Companion.renderer
    get() = rendererFor<ImplicitOAuthSecurity> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(it.name to obj(
                    "type" to string("oauth2"),
                    "flows" to obj("implicit" to
                        obj(
                            listOfNotNull(
                                "authorizationUrl" to string(it.authorizationUrl.toString()),
                                it.refreshUrl?.let { "refreshUrl" to string(it.toString()) },
                                "scopes" to obj(it.scopes.map { it.name to string(it.description) }
                                )
                            ) + it.extraFields.map { it.key to string(it.value) }
                        )
                    )
                ))
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(it.scopes.map { string(it.name) })) }
        }
    }

val UserCredentialsOAuthSecurity.Companion.renderer
    get() = rendererFor<UserCredentialsOAuthSecurity> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(it.name to obj(
                    "type" to string("oauth2"),
                    "flows" to obj("password" to
                        obj(
                            listOfNotNull(
                                it.refreshUrl?.let { "refreshUrl" to string(it.toString()) },
                                "tokenUrl" to string(it.tokenUrl.toString()),
                                "scopes" to obj(it.scopes.map { it.name to string(it.description) }
                                )
                            ) + it.extraFields.map { it.key to string(it.value) }
                        )
                    )
                ))
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(it.scopes.map { string(it.name) })) }
        }
    }

val ClientCredentialsOAuthSecurity.Companion.renderer
    get() = rendererFor<ClientCredentialsOAuthSecurity> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(it.name to obj(
                    "type" to string("oauth2"),
                    "flows" to obj("clientCredentials" to
                        obj(
                            listOfNotNull(
                                it.refreshUrl?.let { "refreshUrl" to string(it.toString()) },
                                "tokenUrl" to string(it.tokenUrl.toString()),
                                "scopes" to obj(it.scopes.map { it.name to string(it.description) }
                                )
                            ) + it.extraFields.map { it.key to string(it.value) }
                        )
                    )
                ))
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(it.scopes.map { string(it.name) })) }
        }
    }

val OpenIdConnectSecurity.Companion.renderer
    get() = rendererFor<OpenIdConnectSecurity> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(
                    it.name to
                        obj(
                            listOfNotNull(
                                "type" to string("openIdConnect"),
                                "openIdConnectUrl" to string(it.discoveryUrl.toString()),
                            )
                        )
                )
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(emptyList())) }
        }
    }
