package dev.forkhandles.values

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success

fun <T, P> Validator<T, P>.resultOf(value: T): Result<T, P> =
    this(value)?.let { Failure(it) } ?: Success(value)
