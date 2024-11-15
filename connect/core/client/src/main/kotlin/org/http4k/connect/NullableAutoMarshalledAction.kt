package org.http4k.connect

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.format.AutoMarshalling
import kotlin.reflect.KClass

abstract class NullableAutoMarshalledAction<R : Any>(private val clazz: KClass<R>, private val json: AutoMarshalling) :
    Action<Result<R?, RemoteFailure>> {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(json.asA(bodyString().takeIf { it.isNotEmpty() } ?: "{}", clazz))
            status == NOT_FOUND -> Success(null)
            else -> Failure(asRemoteFailure(this))
        }
    }
}
