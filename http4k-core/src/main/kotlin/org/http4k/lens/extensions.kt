package org.http4k.lens

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success

/**
 * Convert the result of a lens extraction to a Result4k type which
 */
fun <IN, OUT> LensExtractor<IN, OUT>.asResult(): LensExtractor<IN, Result<OUT, LensFailure>> = object : LensExtractor<IN, Result<OUT, LensFailure>> {
    override fun invoke(target: IN): Result<OUT, LensFailure> = try {
        Success(this@asResult.invoke(target))
    } catch (e: LensFailure) {
        Failure(e)
    }
}
