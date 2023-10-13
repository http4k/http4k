package org.http4k.connect.amazon.lambda

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.connect.amazon.RemoteFailure
import org.http4k.connect.amazon.kClass

class ListFunctions : LambdaAction<List<FunctionDetails>>(kClass()) {
    override fun toRequest() = Request(Method.GET, Uri.of("/2015-03-31/functions/"))

    override fun toResult(response: Response): Result<List<FunctionDetails>, RemoteFailure> =
        response.toActionResult(toRequest())
            .map {
                LambdaJackson.asA<ListFunctionsResponse>(it.bodyString()).functions
                    .map { f -> FunctionDetails(f.arn, f.name) }
            }
}
