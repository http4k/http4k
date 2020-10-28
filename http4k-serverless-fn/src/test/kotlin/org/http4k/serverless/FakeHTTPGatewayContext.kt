package org.http4k.serverless

import com.fnproject.fn.api.Headers
import com.fnproject.fn.api.QueryParameters
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.queries
import java.util.Optional

class FakeHTTPGatewayContext(private val request: Request) : HTTPGatewayContext {
    var response = Response(INTERNAL_SERVER_ERROR)

    override fun getInvocationContext() = TODO("Not yet implemented")

    override fun getHeaders(): Headers = Headers.fromMap(request.headers.toMap())

    override fun getRequestURL(): String = request.uri.toString()

    override fun getMethod(): String = request.method.name

    override fun getQueryParameters(): QueryParameters = object : QueryParameters {
        override fun get(key: String) = Optional.ofNullable(request.query(key))

        override fun getValues(key: String) = request.queries(key)
            .map { it ?: "" }
            .toMutableList()

        override fun getAll(): MutableMap<String, MutableList<String>> {


            return request.uri.queries()
                .map { it.first to (it.second ?: "") }
                .groupBy { it.first }
                .mapValues { it.value.map { it.second }.toMutableList() }.toMutableMap()
        }
    }

    override fun addResponseHeader(key: String, value: String) {
        response = response.header(key, value)
    }

    override fun setResponseHeader(key: String, v1: String, vararg vs: String) {
        response = (listOf(v1) + vs.toList()).fold(response) {
            acc, s -> acc.header(key, s)
        }
    }

    override fun setStatusCode(code: Int) {
        response = response.status(Status(code, ""))
    }

}
