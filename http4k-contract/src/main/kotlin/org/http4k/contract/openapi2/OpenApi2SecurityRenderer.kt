package org.http4k.contract.openapi2

import org.http4k.contract.ApiKeySecurity
import org.http4k.contract.BasicAuthSecurity
import org.http4k.contract.NoSecurity
import org.http4k.contract.Render
import org.http4k.contract.Security
import org.http4k.contract.SecurityRenderer

object OpenApi2SecurityRenderer : SecurityRenderer {
    override fun <NODE> full(security: Security): Render<NODE>? =
        when (security) {
            is BasicAuthSecurity -> {
                {
                    obj(security.name to obj("type" to string("basic")))
                }
            }
            is ApiKeySecurity<*> -> {
                {
                    obj(security.name to obj(
                        "type" to string("apiKey"),
                        "in" to string(security.param.meta.location),
                        "name" to string(security.param.meta.name)
                    ))
                }
            }
            is NoSecurity -> {
                {
                    nullNode()
                }
            }
            else -> null
        }

    override fun <NODE> ref(security: Security): Render<NODE>? =
        when (security) {
            is ApiKeySecurity<*> -> {
                { obj(security.name to array(emptyList())) }
            }
            is BasicAuthSecurity -> {
                { obj(security.name to array(emptyList())) }
            }
            else -> null
        }
}