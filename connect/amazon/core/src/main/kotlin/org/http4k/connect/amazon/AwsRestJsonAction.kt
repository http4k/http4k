package org.http4k.connect.amazon

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.format.AutoMarshalling
import org.http4k.lens.Header
import kotlin.reflect.KClass

abstract class AwsRestJsonAction<R : Any>(
    private val base: Request,
    private val clazz: KClass<R>,
    private val autoMarshalling: AutoMarshalling
) : Action<Result4k<R, RemoteFailure>> {
    override fun toRequest() = base
        .with(Header.CONTENT_TYPE of APPLICATION_JSON)
        .body(autoMarshalling.asFormatString(this))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(autoMarshalling.asA(
                bodyString().takeIf { it.isNotEmpty() } ?: "{}",
                clazz))

            else -> Failure(asRemoteFailure(this))
        }
    }
}
