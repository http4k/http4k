package org.http4k.client

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

    override fun inject(it: Request): (Request) -> Request = requestLens of it.bodyString()

    override fun extract(lambdaResponse: Response) = Response(Status.OK).body(responseLens(lambdaResponse))

    private val requestLens = Body.string(ContentType.TEXT_PLAIN).toLens()
    private val responseLens = Body.string(ContentType.TEXT_PLAIN).toLens()

}
