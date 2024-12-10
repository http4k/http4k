package org.http4k.connect.amazon

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class AwsJsonAction<R : Any>(
    private val service: AwsService,
    private val clazz: KClass<R>,
    private val autoMarshalling: AutoMarshalling,
    private val contentType: ContentType = ContentType("application/x-amz-json-1.1")
) : Action<Result<R, RemoteFailure>> {
    protected open val actionName: String = javaClass.simpleName

    override fun toRequest() = Request(POST, uri())
        .header("X-Amz-Target", "${service}.${actionName}")
        .replaceHeader("Content-Type", contentType.value)
        .body(autoMarshalling.asFormatString(this))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(autoMarshalling.asA(
                bodyString().takeIf { it.isNotEmpty() } ?: "{}",
                clazz))

            else -> Failure(asRemoteFailure(this))
        }
    }

    protected fun uri() = Uri.of("/")
}
