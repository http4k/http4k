package org.http4k.serverless.lambda.testing.setup.aws.lambda

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.serverless.lambda.testing.setup.aws.kClass

class ListFunctions : LambdaAction<ListFunctionsResponse>(kClass()) {
    override fun toRequest() = Request(Method.GET, Uri.of("/2015-03-31/functions/"))
}
