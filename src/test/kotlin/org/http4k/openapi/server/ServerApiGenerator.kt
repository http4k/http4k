package org.http4k.openapi.server

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OPERATOR
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.TypeSpec.Companion.classBuilder
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.httpHandler
import org.http4k.poet.Property
import org.http4k.poet.Property.Companion.addParameter

object ServerApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec) = with(spec) {
        val className = spec.info.title.capitalize() + "Server"

        val server = classBuilder(className.capitalize())
            .addSuperinterface(httpHandler.type)
            .addFunction(invokeMethod())
            .build()

        listOf(
            FileSpec.builder("", server.name!!)
                .addType(server)
                .build()
        )
    }

    private fun OpenApi3Spec.invokeMethod() =
        buildEndpoints().fold(
            FunSpec.builder("invoke")
                .addModifiers(OVERRIDE, OPERATOR)
                .returns(Property<Response>().type)
                .addParameter(Property<Request>())
                .addCode("return Response(org.http4k.core.Status.OK)")
                .build()) { acc, next ->
            acc
        }

    private fun OpenApi3Spec.buildEndpoints() = paths.flatMap {

        val path = it.key
        it.value.entries.map {
            val functionName = it.value.operationId ?: it.key + path.replace('/', '_')
            FunSpec.builder(functionName).build()
        }
    }.sortedBy { it.name }
}
