package org.http4k.serverless

import org.http4k.filter.ZipkinTraces
import org.http4k.filter.ZipkinTracesStorage
import org.http4k.filter.ensureCurrentSpan
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

    /**
     * Initialise request tracing ( if not already instantiated)
     */
    fun <In, Ctx, Out> RequestTracing(
        startReportFn: (In, ZipkinTraces) -> Unit = { _, _ -> },
        endReportFn: (In, Out, ZipkinTraces) -> Unit = { _, _, _ -> },
        storage: ZipkinTracesStorage = ZipkinTracesStorage.THREAD_LOCAL
    ) = FnFilter<In, Ctx, Out> { next ->
        FnHandler { `in`, ctx ->
            storage.ensureCurrentSpan {
                val fromRequest = storage.forCurrentThread()
                startReportFn(`in`, fromRequest)
                storage.setForCurrentThread(fromRequest)
                next(`in`, ctx).apply { endReportFn(`in`, this, fromRequest) }
            }
        }
    }
}
