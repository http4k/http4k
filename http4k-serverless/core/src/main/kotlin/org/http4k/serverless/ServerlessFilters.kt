package org.http4k.serverless

import java.time.Clock
import java.time.Duration

object ServerlessFilters {
    fun <In, Ctx, Out> ReportFnTransaction(
        clock: Clock = Clock.systemUTC(),
        transactionLabeler: FnTransactionLabeler<In, Out> = { it },
        recordFn: (FnTransaction<In, Out>) -> Unit
    ) = FnFilter<In, Ctx, Out> { next ->
        FnHandler { `in`, ctx ->
            clock.instant().let { start ->
                next(`in`, ctx).apply {
                    recordFn(
                        transactionLabeler(FnTransaction(`in`, this, Duration.between(start, clock.instant())))
                    )
                }
            }
        }
    }
}
