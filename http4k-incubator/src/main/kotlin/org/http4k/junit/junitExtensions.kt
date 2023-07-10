package org.http4k.junit

import `in`.specmatic.core.HttpRequest
import `in`.specmatic.core.HttpResponse
import `in`.specmatic.core.NamedStub
import `in`.specmatic.core.value.EmptyString
import `in`.specmatic.mock.ScenarioStub
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.body.form
import org.http4k.core.queries
import org.http4k.core.then
import org.http4k.specmatic.InteractionStorage
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * JUnit 5 extension for recording HTTP traffic to disk in Specmatic format.
 */
class SpecmaticRecording(
    private val storage: InteractionStorage,
    private val httpHandler: HttpHandler,
) : ParameterResolver, BeforeTestExecutionCallback, AfterTestExecutionCallback {

    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.isHttpHandler()

    private var inTest = false
    private val stubs = mutableListOf<NamedStub>()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext) = when {
        pc.isHttpHandler() && inTest -> RecordStub().then(httpHandler)
        else -> httpHandler
    }

    private fun RecordStub() = Filter { next ->
        { req ->
            next(req)
                .also {
                    stubs.add(
                        NamedStub("${req.method} ${req.uri}", ScenarioStub(req.toSpecmatic(), it.toSpecmatic()))
                    )
                }
        }
    }

    override fun beforeTestExecution(context: ExtensionContext?) {
        inTest = true
    }

    override fun afterTestExecution(context: ExtensionContext) {
        inTest = false
        storage.store(stubs)
    }

    private fun Request.toSpecmatic() = HttpRequest(
        method.name,
        uri.path,
        headers.filterNot { it.second == null }.toMap().mapValues { it.value!! },
        EmptyString,
        uri.queries().filterNot { it.second == null }.toMap().mapValues { it.value!! },
        form().filterNot { it.second == null }.toMap().mapValues { it.value!! },
        emptyList()
    )

    private fun Response.toSpecmatic() = HttpResponse(
        status.code,
        bodyString(),
        headers.groupBy { it.first }.mapValues { it.value.map { it.second }.joinToString(",") },
    )

    private fun ParameterContext.isHttpHandler() =
        parameter.parameterizedType.typeName == "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"

}

