package org.http4k.openapi.v3.client

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.CodeBlock.Companion.of
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.asClassName
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.openapi.v3.ParameterSpec
import org.http4k.openapi.v3.Path
import org.http4k.poet.Property
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.asTypeName
import org.http4k.poet.lensDeclaration
import org.http4k.poet.lensDeclarations
import org.http4k.poet.packageMember
import org.http4k.poet.quotedName

fun Path.function(): FunSpec =
    with(this) {
        val reifiedPath = urlPathPattern.replace("/{", "/\${")

        val with = packageMember<Filter>("with")
//
//        val bodyBindings = listOfNotNull(
//            responseSchemas()
//                .firstOrNull()
//                ?.lensDeclaration()
//        )

        val parameterBindings = pathSpec.parameters.mapNotNull {
            val binding = "${it.name}Lens of ${it.name}"

            when (it) {
                is ParameterSpec.CookieSpec -> {
                    val optionality = if (it.required) "" else " ?: \"\""
                    of("\n.%M(${it.name}Lens of %T(${it.quotedName()}, ${it.name}$optionality))", with, Cookie::class.asClassName())
                }
                is ParameterSpec.HeaderSpec -> of("\n.%M($binding)", with)
                is ParameterSpec.QuerySpec -> of("\n.%M($binding)", with)
                else -> null
            }
        }

        val buildRequest = (parameterBindings)
            .fold(CodeBlock.builder()
                .add("val request = %T(%T.$method,·\"$reifiedPath\")", Property<Request>().type, Property<Method>().type)) { acc, next ->
                acc.add(next)
            }.build()

        val responseType = responseSchemas().firstOrNull()?.let { ClassName("", it.name) } ?: Unit::class.asClassName()

        val response = responseSchemas().firstOrNull()?.let { schema ->
            schema.lensDeclaration()
                ?.let { listOf(of("return " + schema.name.decapitalize() + "(httpHandler(request))")) }
                ?: emptyList()
        } ?: listOf(of("\nhttpHandler(request)"))

        FunSpec.builder(uniqueName.decapitalize())
            .addAllParametersFrom(this)
            .returns(responseType)
            .addCodeBlocks(lensDeclarations())
            .addCode(buildRequest)
            .addCodeBlocks(response)
            .build()
    }

private fun FunSpec.Builder.addAllParametersFrom(path: Path): FunSpec.Builder =
    with(path) {
        val parameters = pathSpec.parameters.map { it.name to it.asTypeName()!! }

        val bodyParams = requestSchemas().map { "request" to ClassName("", it.name) }

        (parameters + bodyParams).fold(this@addAllParametersFrom) { acc, next ->
            acc.addParameter(next.first, next.second)
        }
    }
