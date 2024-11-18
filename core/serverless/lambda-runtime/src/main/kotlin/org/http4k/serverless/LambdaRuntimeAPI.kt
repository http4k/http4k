package org.http4k.serverless

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.lens.Header
import org.http4k.lens.long
import org.http4k.lens.string
import org.http4k.lens.uuid
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.Instant.ofEpochMilli

/**
 * Client API to retrieve requests and post responses back to the AWS Lambda controller.
 */
class LambdaRuntimeAPI(http: HttpHandler) {

    private val safeHttp = Filter { next ->
        {
            next(it).apply {
                if (!status.successful) error("""Lambda Runtime API error calling ${it.uri}: $body""")
            }
        }
    }.then(http)

    fun nextInvocation() =
        with(safeHttp(Request(GET, "/2018-06-01/runtime/invocation/next"))) {
            Request(POST, "").body(body).headers(headers)
        }

    fun success(invocation: Request, stream: InputStream) {
        safeHttp(Request(POST, "/2018-06-01/runtime/invocation/${requestId(invocation)}/response").body(stream))
    }

    fun error(invocation: Request, exception: Exception) {
        safeHttp(
            Request(POST, "/2018-06-01/runtime/invocation/${requestId(invocation)}/error")
                .body(exception.toBody())
        )
    }

    fun initError(error: Exception) {
        safeHttp(
            Request(POST, "/2018-06-01/runtime/init/error")
                .header("Lambda-Runtime-Function-Error-Type", "Unhandled")
                .body(error.toBody())
        )
    }

    companion object {
        val requestId = Header.uuid().required("Lambda-Runtime-Aws-Request-Id")
        val deadline = Header.long().map(::ofEpochMilli, Instant::toEpochMilli).required("Lambda-Runtime-Deadline-Ms")
        val lambdaArn = Header.string().required("Lambda-Runtime-Invoked-Function-Arn")
        val traceId = Header.string().required("Lambda-Runtime-Trace-Id")
    }
}

internal fun Exception.toBody() =
    StringWriter().use { output ->
        PrintWriter(output).use { printer ->
            printStackTrace(printer)
            output.toString()
        }
    }
