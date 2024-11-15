package org.http4k.connect

import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.Response
import dev.forkhandles.result4k.Result as Result4k

/**
 * Transparent Action adapter from standard -> Result4k monad
 */
abstract class Result4kAction<R, T : Action<R>>(private val action: T) : Action<Result4k<R, RemoteFailure>> {
    override fun toResult(response: Response) = resultFrom {
        action.toResult(response)
    }.mapFailure {
        toRequest().run {
            RemoteFailure(method, uri, response.status, it.localizedMessage)
        }
    }

    override fun toRequest() = action.toRequest()
    override fun hashCode() = action.hashCode()
    override fun toString() = action.toString()
    override fun equals(other: Any?) = action == other
}

/**
 * Transparent Action adapter from standard -> Kotlin Result monad
 */
abstract class ResultAction<R, T : Action<R>>(private val action: T) : Action<Result<R>> {
    override fun toResult(response: Response) = try {
        Result.success(action.toResult(response))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun toRequest() = action.toRequest()
    override fun equals(other: Any?) = action == other
    override fun hashCode() = action.hashCode()
    override fun toString() = action.toString()
}
