package org.http4k.contract.openapi.v2

import org.http4k.contract.openapi.Render
import org.http4k.contract.openapi.RenderModes
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.contract.openapi.rendererFor
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.BasicAuthSecurity

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

val OpenApi2SecurityRenderer = SecurityRenderer(ApiKeySecurity.renderer, BasicAuthSecurity.renderer)
