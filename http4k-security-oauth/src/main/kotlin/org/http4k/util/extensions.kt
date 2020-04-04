package org.http4k.util

/**
 * converter method to Result4k
 */
val <E, T> Result<E, T>.asResult4k: com.natpryce.Result<T, E> get() = when(this) {
    is Success -> com.natpryce.Success(value)
    is Failure -> com.natpryce.Failure(reason)
}

/**
 * Unwrap a Result, by returning the success value or calling _block_ on failure to abort from the current function.
 */
inline fun <T, E> Result<E, T>.onFailure(block: (Failure<E>) -> Nothing): T = when (this) {
    is Success<T> -> value
    is Failure<E> -> block(this)
}
