package org.http4k.contract.openapi2

import org.http4k.contract.ApiKeySecurity
import org.http4k.contract.BasicAuthSecurity
import org.http4k.contract.Security
import org.http4k.contract.SecurityRenderer
import org.http4k.format.Json

object OpenApi2SecurityRenderer : SecurityRenderer {
    override fun <NODE> full(security: Security): Json<NODE>.() -> NODE = {
        when (security) {
            is BasicAuthSecurity -> obj(
                security.name to obj(
                    "type" to string("basic")
                )
            )
            is ApiKeySecurity<*> -> obj(
                security.name to obj(
                    "type" to string("apiKey"),
                    "in" to string(security.param.meta.location),
                    "name" to string(security.param.meta.name)
                ))
            else -> obj(listOf())
        }
    }

    override fun <NODE> ref(security: Security): Json<NODE>.() -> NODE = {
        array(
            when (security) {
                is ApiKeySecurity<*> -> listOf(obj(security.name to array(emptyList())))
                is BasicAuthSecurity -> listOf(obj(security.name to array(emptyList())))
                else -> emptyList()
            }
        )
    }
}