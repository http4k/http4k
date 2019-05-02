package org.http4k.contract

import org.http4k.format.Json

interface SecurityRenderer<NODE> {
    fun full(security: Security): NODE
    fun ref(security: Security): NODE

    companion object
}

fun <NODE> SecurityRenderer.Companion.OpenApi(json: Json<NODE>): SecurityRenderer<NODE> = object : SecurityRenderer<NODE> {
    override fun full(security: Security) = json {
        when (security) {
            is BasicAuthSecurity -> obj(
                "basicAuth" to obj(
                    "type" to string("basic")
                )
            )
            is ApiKeySecurity<*> -> obj(
                "api_key" to obj(
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