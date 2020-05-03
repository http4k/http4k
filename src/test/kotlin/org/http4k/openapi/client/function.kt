package org.http4k.openapi.client

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.CodeBlock.Companion.of
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.lens.Cookies
import org.http4k.lens.Header
import org.http4k.lens.Query
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.ParameterSpec
import org.http4k.openapi.PathSpec
import org.http4k.poet.Property
import org.http4k.poet.Property.Companion.addReturnType
import org.http4k.poet.asTypeName

fun OpenApi3Spec.function(path: String, method: Method, pathSpec: PathSpec): FunSpec {
    val functionName = pathSpec.operationId ?: method.name.toLowerCase() + path.replace('/', '_')

    val reifiedPath = pathSpec.parameters.filterIsInstance<ParameterSpec.PathSpec>()
        .fold(path) { acc, next -> acc.replace("/{", "/\${") }

    val map = pathSpec.parameters.mapNotNull {
        val value = "${it.name}${ if(it.required) "" else "?" }.toString()"
        val binding = "%T.%M().optional(${it.quotedName()}) of $value"
        val with = MemberName("org.http4k.core", "with")
        val string = MemberName("org.http4k.lens", "string")

        when (it) {
            is ParameterSpec.CookieSpec ->
                of("\n.%M(%T.optional(${it.quotedName()}) of %T(${it.quotedName()}, $value ?: \"\"))", with, Cookies::class.asClassName(), Cookie::class.asClassName())
            is ParameterSpec.HeaderSpec -> of("\n.%M($binding)", with, Header::class.asClassName(), string)
            is ParameterSpec.QuerySpec -> of("\n.%M($binding)", with, Query::class.asClassName(), string)
            else -> null
        }
    }

    val request =  map.fold(CodeBlock.builder().add("val request = %T(%T.$method,·\"$reifiedPath\")", Property<Request>().type, Property<Method>().type)) {
        acc, next -> acc.add(next)
    }.build()

    return pathSpec.parameters
        .fold(FunSpec.builder(functionName)) { acc, next ->
            acc.addParameter(next.name, next.asTypeName()!!)
        }
        .addReturnType(Property<Response>())
        .addCode(request)
        .addCode("\nreturn·httpHandler(request)")
        .build()
}

private fun ParameterSpec.quotedName() = "\"$name\""
