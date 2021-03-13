package org.http4k.serverless.lambda.testing.client

import org.http4k.aws.Function
import org.http4k.aws.Region
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.string

class InvocationLambdaClient(function: Function, region: Region) :
    LambdaHttpClient(function, region) {

    override fun Request.toLambdaFormat(): (Request) -> Request = requestLens of bodyString()

    override fun Response.fromLambdaFormat() = Response(Status.OK).body(responseLens(this))

    private val requestLens = Body.string(ContentType.TEXT_PLAIN).toLens()
    private val responseLens = Body.string(ContentType.TEXT_PLAIN).toLens()
}
