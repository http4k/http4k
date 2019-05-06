package org.http4k.contract.openapi3

import org.http4k.contract.ApiKeySecurity
import org.http4k.contract.BasicAuthSecurity
import org.http4k.contract.BearerAuthSecurity
import org.http4k.contract.Security
import org.http4k.contract.SecurityRenderer
import org.http4k.format.Json

class OpenApi3SecurityRenderer<NODE>(private val json: Json<NODE>) : SecurityRenderer<NODE> {
    override fun full(security: Security) = json {
        when (security) {
            is BasicAuthSecurity -> obj(
                security.name to obj(
                    "scheme" to string("basic"),
                    "type" to string("http")
                )
            )
            is BearerAuthSecurity -> obj(
                security.name to obj(
                    "scheme" to string("bearer"),
                    "type" to string("http")
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
                is ApiKeySecurity<*> -> listOf(obj(security.name to array(emptyList())))
                is BasicAuthSecurity -> listOf(obj(security.name to array(emptyList())))
                is BearerAuthSecurity -> listOf(obj(security.name to array(emptyList())))
                else -> emptyList()
            }
        )
    }
}