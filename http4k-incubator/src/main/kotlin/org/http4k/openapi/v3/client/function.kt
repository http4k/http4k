package org.http4k.openapi.v3.client

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.CodeBlock.Companion.of
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.asClassName
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.ParameterSpec
import org.http4k.openapi.v3.PathSpec
import org.http4k.poet.Property
import org.http4k.poet.Property.Companion.addReturnType
import org.http4k.poet.addCodeBlocks
import org.http4k.poet.asTypeName
import org.http4k.poet.lensDeclarations
import org.http4k.poet.packageMember
import org.http4k.poet.quotedName

fun OpenApi3Spec.function(path: String, method: Method, pathSpec: PathSpec): FunSpec {
    val functionName = pathSpec.operationId ?: method.name.toLowerCase() + path.replace('/', '_')

    val reifiedPath = path.replace("/{", "/\${")

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

    return pathSpec.parameters
        .fold(FunSpec.builder(functionName)) { acc, next ->
            acc.addParameter(next.name, next.asTypeName()!!)
        }
        .addReturnType(Property<Response>())
        .addCodeBlocks(lensDeclarations(pathSpec))
        .addCode(request)
        .addCode("\nreturn·httpHandler(request)")
        .build()
}
