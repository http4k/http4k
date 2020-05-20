package org.http4k.openapi.v3.client

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec.Companion.constructorBuilder
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Companion.classBuilder
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.apiName
import org.http4k.openapi.v3.flatten
import org.http4k.openapi.v3.flattenedPaths
import org.http4k.poet.Property
import org.http4k.poet.Property.Companion.addParameter
import org.http4k.poet.Property.Companion.addProperty
import org.http4k.poet.buildFormatted

object ClientApiGenerator : ApiGenerator<OpenApi3Spec> {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions) =
        with(spec.flatten()) {
            val httpHandler = Property("org.http4k.core.HttpHandler", false, PRIVATE)

            val className = apiName() + "Client"

            val clientCode = flattenedPaths()
                .map { it.function(options.packageName("model")) }
                .fold(classBuilder(className), TypeSpec.Builder::addFunction)
                .addProperty(httpHandler)
                .primaryConstructor(constructorBuilder().addParameter(httpHandler).build())
                .build()

            listOf(
                FileSpec.builder(options.packageName("client"), className)
                    .addType(clientCode)
                    .buildFormatted()
            )
        }
}

