package org.http4k.connect.amazon.sqs

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.AutoMarshalling
import org.http4k.lens.Header.CONTENT_TYPE
import kotlin.reflect.KClass
import dev.forkhandles.result4k.Success

private val amazonJson = ContentType("application/x-amz-json-1.0")

abstract class SQSAction<ResultOut: Any, ResponseBody: Any>(
    private val action: String,
    private val clazz: KClass<ResponseBody>,
    private val resultFn: (ResponseBody) -> ResultOut,
    private val autoMarshalling: AutoMarshalling = SqsMoshi
) : Action<Result<ResultOut, RemoteFailure>> {

    override fun toRequest() = Request(POST, Uri.of(""))
        .header("X-Amz-Target", "AmazonSQS.${action}")
        .with(CONTENT_TYPE of amazonJson)
        .body(autoMarshalling.asFormatString(requestBody()))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> autoMarshalling
                .asA(bodyString().takeIf { it.isNotEmpty() } ?: "{}", clazz)
                .let(resultFn)
                .let(::Success)

            else -> Failure(asRemoteFailure(this))
        }
    }

    open fun requestBody(): Any = this
}
