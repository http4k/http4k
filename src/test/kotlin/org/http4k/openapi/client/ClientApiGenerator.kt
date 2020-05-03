package org.http4k.openapi.client

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec.Companion.constructorBuilder
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Companion.classBuilder
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.httpHandler
import org.http4k.poet.Property.Companion.addParameter
import org.http4k.poet.Property.Companion.addProperty

object ClientApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions) =
        with(spec) {
            val className = info.title.capitalize() + "Client"

            val clientCode = functions().fold(classBuilder(className), TypeSpec.Builder::addFunction)
                .addProperty(httpHandler)
                .primaryConstructor(constructorBuilder().addParameter(httpHandler).build())
                .build()

            listOf(
                FileSpec.builder(options.packageName("client"), className)
                    .addType(clientCode)
                    .indent("\t")
                    .build()
            )
        }

}

