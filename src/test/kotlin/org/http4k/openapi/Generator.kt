package org.http4k.openapi

import org.http4k.core.Method
import java.io.File

fun main(args: Array<String>) {
    val api = OpenApiJson.asA(File(args[0]).readText(), OpenApi3Spec::class)
    api.paths.map {
        val path = it.key
        println(it.value.entries.map {
            Method.valueOf(it.key.toUpperCase()) to (it.value.operationId ?: path + it.key)
        })
    }
    println(api)
}
