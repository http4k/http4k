package org.http4k.connect

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Response
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class NonNullAutoMarshalledAction<R : Any>(
    private val clazz: KClass<R>,
    protected val autoMarshalling: AutoMarshalling
) : Action<Result<R, RemoteFailure>> {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(autoMarshalling.asA(bodyString().takeIf { it.isNotEmpty() } ?: "{}", clazz))
            else -> Failure(asRemoteFailure(this))
        }
    }
}
