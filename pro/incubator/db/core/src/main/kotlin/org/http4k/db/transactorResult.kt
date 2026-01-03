package org.http4k.db

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.db.Transactor.Mode
import org.http4k.db.Transactor.Mode.ReadWrite

fun <T, Resource> Transactor<Resource>.performAsResult(mode: Mode = ReadWrite, work: (Resource) -> T): Result<T, Exception> =
    try {
        Success(perform(mode, work))
    } catch (e: Exception) {
        Failure(e)
    }

