package org.http4k.contract.openapi3

import org.http4k.contract.ApiKeySecurity
import org.http4k.contract.BasicAuthSecurity
import org.http4k.contract.Security
import org.http4k.contract.SecurityRenderer
import org.http4k.format.Json

fun <NODE> OpenApi3SecurityRenderer(json: Json<NODE>): SecurityRenderer<NODE> = object : SecurityRenderer<NODE> {
    override fun full(security: Security) = json {
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

    override fun ref(security: Security) = json {
        array(
            when (security) {
                is ApiKeySecurity<*> -> listOf(obj("api_key" to array(emptyList())))
                is BasicAuthSecurity -> listOf(obj("basicAuth" to array(emptyList())))
                else -> emptyList()
            }
        )
    }
}