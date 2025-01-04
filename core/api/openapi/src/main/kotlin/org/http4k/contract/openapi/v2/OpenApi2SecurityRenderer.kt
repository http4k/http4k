package org.http4k.contract.openapi.v2

import org.http4k.contract.openapi.Render
import org.http4k.contract.openapi.RenderModes
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.contract.openapi.rendererFor
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.contract.security.ImplicitOAuthSecurity

/**
 * Compose the supported Security models
 */
val OpenApi2SecurityRenderer = SecurityRenderer(ApiKeySecurity.renderer, BasicAuthSecurity.renderer, ImplicitOAuthSecurity.renderer)

val ApiKeySecurity.Companion.renderer
    get() = rendererFor<ApiKeySecurity<*>> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(it.name to obj(
                    "type" to string("apiKey"),
                    "in" to string(it.param.meta.location),
                    "name" to string(it.param.meta.name)
                ))
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(emptyList())) }
        }
    }

val BasicAuthSecurity.Companion.renderer
    get() = rendererFor<BasicAuthSecurity> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(it.name to obj(
                    "type" to string("basic")
                ))
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(emptyList())) }
        }
    }

val ImplicitOAuthSecurity.Companion.renderer
    get() = rendererFor<ImplicitOAuthSecurity> {
        object : RenderModes {
            override fun <NODE> full(): Render<NODE> = {
                obj(it.name to
                    obj(
                        listOfNotNull(
                            "type" to string("oauth2"),
                            "flow" to string("implicit"),
                            "authorizationUrl" to string(it.authorizationUrl.toString())
                        ) + it.extraFields.map { it.key to string(it.value) }
                    )
                )
            }

            override fun <NODE> ref(): Render<NODE> = { obj(it.name to array(emptyList())) }
        }
    }
