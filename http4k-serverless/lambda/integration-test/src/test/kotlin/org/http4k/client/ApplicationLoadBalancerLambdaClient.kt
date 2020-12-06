package org.http4k.client

import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent
import org.http4k.aws.Function
import org.http4k.aws.Region
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.toParameters
import org.http4k.format.Jackson.auto

class ApplicationLoadBalancerLambdaClient(function: Function, region: Region) : LambdaHttpClient(function, region) {
    override fun Request.toLamdbaFormat(): (Request) -> Request = requestLens of ApplicationLoadBalancerRequestEvent().apply {
        httpMethod = method.name
        body = bodyString()
        headers = this@toLamdbaFormat.headers.toMap()
        path = uri.path
        queryStringParameters = uri.query.toParameters().toMap()
    }

    override fun Response.fromLambdaFormat(): Response {
        val response = responseLens(this)
        return Response(Status(response.statusCode, ""))
                .headers((response.headers
                    ?: emptyMap()).entries.fold(listOf(), { acc, next -> acc + (next.key to next.value) }))
                .headers((response.multiValueHeaders ?: emptyMap()).entries.fold(listOf(), { acc, next ->
                    next.value.fold(acc, { acc2, next2 -> acc2 + (next.key to next2) })
                }))
                .body(response.body.orEmpty())
    }

    private val requestLens = Body.auto<ApplicationLoadBalancerRequestEvent>().toLens()
    private val responseLens = Body.auto<ApplicationLoadBalancerResponseEvent>().toLens()
}
