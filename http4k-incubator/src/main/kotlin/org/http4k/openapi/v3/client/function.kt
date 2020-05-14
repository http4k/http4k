package org.http4k.openapi.v3.client

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.CodeBlock.Companion.of
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.asClassName
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.ParameterSpec
import org.http4k.openapi.v3.Path
import org.http4k.openapi.v3.models.buildModelClass
import org.http4k.poet.Property
import org.http4k.poet.Property.Companion.addReturnType
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.asTypeName
import org.http4k.poet.lensDeclarations
import org.http4k.poet.packageMember
import org.http4k.poet.quotedName

fun OpenApi3Spec.function(path: Path): FunSpec =
    with(path) {
        val reifiedPath = urlPathPattern.replace("/{", "/\${")

        val messageBindings = pathSpec.parameters.mapNotNull {
            val binding = "${it.name}Lens of ${it.name}"
            val with = packageMember<Filter>("with")

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

        val request = messageBindings
            .fold(CodeBlock.builder()
                .add("val request = %T(%T.$method,·\"$reifiedPath\")", Property<Request>().type, Property<Method>().type)) { acc, next ->
                acc.add(next)
            }.build()

        FunSpec.builder(path.uniqueName.decapitalize()).addAllParametersFrom(this)
            .addReturnType(Property<Response>())
            .addCodeBlocks(lensDeclarations(this))
            .addCode(request)
            .addCode("\nreturn·httpHandler(request)")
            .build()
    }

private fun FunSpec.Builder.addAllParametersFrom(path: Path): FunSpec.Builder =
    with(path) {
        val parameters = pathSpec.parameters.map { it.name to it.asTypeName()!! }

        val bodyParams = requestSchemas().map {
            "request" to ClassName("", it.name)
        }

        (parameters + bodyParams).fold(this@addAllParametersFrom) { acc, next ->
            acc.addParameter(next.first, next.second)
        }
    }
