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
import org.http4k.format.AutoMarshalling
import org.http4k.format.AwsLambdaMoshi
import org.http4k.format.Moshi
import kotlin.reflect.KClass

class InvokeFunction<RESP : Any>(
    private val name: FunctionName,
    private val req: Any,
    private val respClass: KClass<RESP>,
    private val autoMarshalling: AutoMarshalling = Moshi
) : LambdaAction<RESP> {
    override fun toRequest() = Request(POST, uri())
        .header("X-Amz-Invocation-Type", "RequestResponse")
        .header("X-Amz-Log-Type", "Tail")
        .body(autoMarshalling.asFormatString(req))

    private fun uri() = Uri.of("/2015-03-31/functions/${name.value}/invocations")

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(autoMarshalling.asA(bodyString(), respClass))
            else -> Failure(asRemoteFailure(this))
        }
    }

    companion object
}

inline fun <reified RESP : Any> Lambda.invokeFunction(
    name: FunctionName,
    req: Any,
    autoMarshalling: AutoMarshalling = AwsLambdaMoshi
) = this(InvokeFunction(name, req, RESP::class, autoMarshalling))
