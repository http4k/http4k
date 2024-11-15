package org.http4k.connect.amazon.lambda.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.connect.amazon.lambda.LambdaAction
import org.http4k.connect.amazon.lambda.model.FunctionName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import java.io.InputStream

class InvokeStreamFunction(
    private val name: FunctionName,
    private val req: InputStream,
) : LambdaAction<InputStream> {
    override fun toRequest() = Request(POST, uri())
        .header("X-Amz-Invocation-Type", "RequestResponse")
        .header("X-Amz-Log-Type", "Tail")
        .body(req)

    private fun uri() = Uri.of("/2015-03-31/functions/${name.value}/invocations")

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(body.stream)
            else -> Failure(asRemoteFailure(this))
        }
    }

    companion object
}

fun Lambda.invokeStreamFunction(name: FunctionName, req: InputStream) = this(InvokeStreamFunction(name, req))
