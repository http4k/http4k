package org.http4k.util

/**
 * A result of a computation that can succeed or fail. Ported from Result4k
 */
sealed class Result<out E, out T> {
    companion object {
        /**
         * Call a function and wrap the result in a Result, catching any Exception and returning it as Err value.
         */
        inline operator fun <T> invoke(block: () -> T): Result<Exception, T> =
            try {
                Success(block())
            } catch (x: Exception) {
                Failure(x)
            }
    }
}

data class Success<out T>(val value: T) : Result<Nothing, T>()
data class Failure<out E>(val reason: E) : Result<E, Nothing>()

/**
 * Map a function over the _value_ of a successful Result.
 */
inline fun <T, NEXT, E> Result<E, T>.map(f: (T) -> NEXT): Result<E, NEXT> =
    flatMap { value -> Success(f(value)) }

/**
 * Flat-map a function over the _value_ of a successful Result.
 */
inline fun <T, NEXT, E> Result<E, T>.flatMap(f: (T) -> Result<E, NEXT>): Result<E, NEXT> =
    when (this) {
        is Success<T> -> f(value)
        is Failure<E> -> this
    }

/**
 * Flat-map a function over the _reason_ of a unsuccessful Result.
 */
inline fun <T, E, NEXT> Result<E, T>.flatMapFailure(f: (E) -> Result<NEXT, T>) = when (this) {
    is Success<T> -> this
    is Failure<E> -> f(reason)
}

/**
 * Map a function over the _reason_ of an unsuccessful Result.
 */
inline fun <T, E, NEXT> Result<E, T>.mapFailure(f: (E) -> NEXT) = flatMapFailure { reason -> Failure(f(reason)) }
