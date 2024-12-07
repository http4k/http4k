package org.http4k.connect.amazon.lambda.action

import com.fasterxml.jackson.annotation.JsonProperty
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.lambda.LambdaAction
import org.http4k.connect.amazon.lambda.LambdaJackson
import org.http4k.connect.amazon.lambda.model.FunctionDetails
import org.http4k.connect.amazon.lambda.toActionResult
import org.http4k.connect.kClass
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

class ListFunctions : LambdaAction<List<FunctionDetails>>(kClass()) {
    override fun toRequest() = Request(Method.GET, Uri.of("/2015-03-31/functions/"))

    override fun toResult(response: Response): Result<List<FunctionDetails>, RemoteFailure> =
        response.toActionResult(toRequest())
            .map {
                LambdaJackson.asA<ListFunctionsResponse>(it.bodyString()).functions
                    .map { f -> FunctionDetails(f.arn, f.name) }
            }
}

data class ListFunctionsResponse(@JsonProperty("Functions") val functions: List<FunctionDetailsData>)
data class FunctionDetailsData(
    @JsonProperty("FunctionArn") val arn: String,
    @JsonProperty("FunctionName") val name: String
)
