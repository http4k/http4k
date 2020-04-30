package org.http4k.openapi.server

import com.squareup.kotlinpoet.FileSpec
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.OpenApi3Spec

object ServerApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec) = with(spec) {
        val name = spec.info.title.capitalize() + "Server"

        listOf(
            FileSpec.builder("", name)
                .build()
        )
    }
}
