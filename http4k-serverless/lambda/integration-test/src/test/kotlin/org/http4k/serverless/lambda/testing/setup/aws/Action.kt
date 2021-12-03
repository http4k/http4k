package org.http4k.serverless.lambda.testing.setup.aws

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.recover
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import kotlin.reflect.KClass

interface Action<R> {
    fun toRequest(): Request
    fun toResult(response: Response): R
}

data class RemoteFailure(val method: Method, val uri: Uri, val status: Status, val message: String? = null) {
    fun throwIt(): Nothing = throw Exception(toString())
}

inline fun <reified T : Any> kClass(): KClass<T> = T::class

fun <T> Result<T, RemoteFailure>.getOrThrow() = recover { it.throwIt() }
