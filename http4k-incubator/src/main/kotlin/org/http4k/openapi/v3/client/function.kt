package org.http4k.openapi.v3.client

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.CodeBlock.Companion.of
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.lens.WebForm
import org.http4k.openapi.NamedSchema
import org.http4k.openapi.SchemaSpec
import org.http4k.openapi.v3.OpenApi3ParameterSpec
import org.http4k.openapi.v3.Path
import org.http4k.poet.Property
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.asTypeName
import org.http4k.poet.buildWebForm
import org.http4k.poet.childClassName
import org.http4k.poet.lensDeclaration
import org.http4k.poet.packageMember
import org.http4k.poet.parameterLensDeclarations
import org.http4k.poet.quotedName
import org.http4k.poet.requestLensDeclarations
import org.http4k.poet.responseLensDeclarations
import org.http4k.poet.webFormLensDeclaration

private const val reqValName = "httpReq"

fun Path.function(modelPackageName: String): FunSpec =
    with(this) {
        val reifiedPath = urlPathPattern.replace("/{", "/\${")

        val parameterBindings = spec.parameters.mapNotNull {
            val binding = "${it.name}Lens of ${it.name}"
            val with = packageMember<Filter>("with")

            when (it) {
                is OpenApi3ParameterSpec.CookieSpec -> {
                    val optionality = if (it.required) "" else " ?: \"\""
                    of("\n\t.%M(${it.name}Lens of %T(${it.quotedName()}, ${it.name}$optionality))", with, Cookie::class.asClassName())
                }
                is OpenApi3ParameterSpec.HeaderSpec -> of("\n\t.%M($binding)", with)
                is OpenApi3ParameterSpec.QuerySpec -> of("\n\t.%M($binding)", with)
                else -> null
            }
        }

        val requestSchemas = when {
            spec.parameters.none { it is OpenApi3ParameterSpec.FormFieldSpec } -> requestSchemas()
            else -> requestSchemas() + NamedSchema.Existing("form", WebForm::class.asClassName())
        }

        val bodyBindings = requestSchemas.bindFirstToHttpMessage("request")

        val buildRequest = (bodyBindings + parameterBindings)
            .fold(CodeBlock.builder()
                .add("val $reqValName = %T(%T.$method,·\"$reifiedPath\")", Property<Request>().type, Property<Method>().type)) { acc, next ->
                acc.add(next)
            }.build()

        val response = responseSchemas().firstOrNull()?.let { schema ->
            schema.lensDeclaration(modelPackageName)
                ?.let { listOf(of("return ${schema.fieldName}Lens(httpHandler($reqValName))")) }
                ?: emptyList()
        } ?: listOf(of("\nhttpHandler($reqValName)"))

        val responseType = responseSchemas().firstOrNull()?.let {
            when (it) {
                is NamedSchema.Generated -> modelPackageName.childClassName(it.fieldName)
                is NamedSchema.Existing -> it.typeName
            }

        } ?: Unit::class.asClassName()

        FunSpec.builder(uniqueName.decapitalize())
            .addAllParametersFrom(this, modelPackageName)
            .returns(responseType)
            .addCodeBlocks((
                requestLensDeclarations(modelPackageName) +
                    responseLensDeclarations(modelPackageName) +
                    parameterLensDeclarations() +
                    webFormLensDeclaration() +
                    buildWebForm()
                )
                .distinct())
            .addCode(buildRequest)
            .addCodeBlocks(response)
            .build()
    }

fun List<NamedSchema>.bindFirstToHttpMessage(input: String) = listOfNotNull(
    firstOrNull()
        ?.let {
            val binding = "${it.fieldName}Lens of $input"
            of("\n\t.%M($binding)", packageMember<Filter>("with"))
        }
)

private fun FunSpec.Builder.addAllParametersFrom(path: Path, modelPackageName: String): FunSpec.Builder =
    with(path) {
        val parameters = spec.parameters.map { it.name to it.asTypeName() }

        val bodyParams = requestSchemas().map {
            "request" to when (it) {
                is NamedSchema.Generated -> {
                    val modelClassName = modelPackageName.childClassName(it.name)
                    when (it.schema) {
                        is SchemaSpec.ArraySpec -> List::class.asClassName().parameterizedBy(modelClassName)
                        else -> modelClassName
                    }
                }
                is NamedSchema.Existing -> it.typeName
            }
        }

        (parameters + bodyParams).fold(this@addAllParametersFrom) { acc, next ->
            acc.addParameter(next.first, next.second)
        }
    }
