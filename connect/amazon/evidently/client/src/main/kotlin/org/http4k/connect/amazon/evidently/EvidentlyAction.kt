package org.http4k.connect.amazon.evidently

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class EvidentlyAction<ResponseBody : Any>(
    private val clazz: KClass<ResponseBody>,
    private val autoMarshalling: AutoMarshalling = EvidentlyMoshi,
    private val method: Method = Method.POST,
    val dataPlane: Boolean = false
) : Action<Result<ResponseBody, RemoteFailure>> {
    abstract fun uri(): Uri

    abstract fun requestBody(): Any

    override fun toRequest() = Request(method, uri())
        .replaceHeader("Content-Type", ContentType.APPLICATION_JSON.value)
        .body(autoMarshalling.asFormatString(requestBody()))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> autoMarshalling
                .asA(bodyString().takeIf { it.isNotEmpty() } ?: "{}", clazz)
                .let(::Success)

            else -> Failure(asRemoteFailure(this))
        }
    }
}
