package org.http4k.connect

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.recover
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri

data class RemoteFailure(val method: Method, val uri: Uri, val status: Status, val message: String? = null) {
    fun throwIt(): Nothing = throw Exception(toString())
}

fun <R> Action<R>.asRemoteFailure(response: Response) =
    with(toRequest()) { RemoteFailure(method, uri, response.status, response.bodyString()) }

fun <T> Result<T, RemoteFailure>.orThrow() = recover { it.throwIt() }
