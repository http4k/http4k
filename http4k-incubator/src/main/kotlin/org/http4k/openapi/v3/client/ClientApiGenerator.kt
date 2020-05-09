package org.http4k.openapi.v3.client

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec.Companion.constructorBuilder
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Companion.classBuilder
import org.http4k.openapi.v3.ApiGenerator
import org.http4k.openapi.v3.GenerationOptions
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.poet.Property
import org.http4k.poet.Property.Companion.addParameter
import org.http4k.poet.Property.Companion.addProperty
import org.http4k.poet.buildFormatted

object ClientApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions) =
        with(spec) {
            val httpHandler = Property("org.http4k.core.HttpHandler", false, KModifier.PRIVATE)

            val className = info.title.capitalize() + "Client"

            val clientCode = functions().fold(classBuilder(className), TypeSpec.Builder::addFunction)
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

