package org.http4k.openapi.server

import com.squareup.kotlinpoet.FileSpec
import org.http4k.core.Method
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.OpenApi3Spec

object ServerApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec) = with(spec) {
        paths.map {
            val path = it.key
            println(it.value.entries.map {
                Method.valueOf(it.key.toUpperCase()) to (it.value.operationId ?: path + it.key)
            })
        }

        emptyList<FileSpec>()
    }
}
