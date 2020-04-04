package org.http4k.util

/**
 * converter method to Result4k
 */
val <E, T> Result<E, T>.asResult4k: com.natpryce.Result<T, E> get() = when(this) {
    is Success -> com.natpryce.Success(value)
    is Failure -> com.natpryce.Failure(reason)
}
