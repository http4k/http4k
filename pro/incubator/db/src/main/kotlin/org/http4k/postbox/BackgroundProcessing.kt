package org.http4k.postbox

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.peek
import dev.forkhandles.result4k.peekFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.db.Transactor
import org.http4k.db.performAsResult
import org.http4k.events.Event
import org.http4k.events.Events
import org.http4k.postbox.ProcessingEvent.BatchProcessingFailed
import org.http4k.postbox.ProcessingEvent.BatchProcessingSucceeded
import org.http4k.postbox.ProcessingEvent.PollWait
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic.markNow


class PostboxProcessing(
    private val transactor: Transactor<Postbox>,
    private val target: HttpHandler,
    private val batchSize: Int = 10,
    private val maxPollingTime: Duration = 5.seconds,
    private val events: Events = { },
    private val executorService: ExecutorService = Executors.newVirtualThreadPerTaskExecutor(),
) : Runnable {
    private var running = true

    override fun run() {
        while (running) {
            val mark = markNow()
            val result = processPendingRequests()
            val elapsedTime = mark.elapsedNow()

            result
                .peek { events(BatchProcessingSucceeded(it, elapsedTime)) }
                .peekFailure { events(BatchProcessingFailed(it.reason)) }

            val remainingTime = maxPollingTime - elapsedTime
            if (remainingTime > Duration.ZERO) {
                events(PollWait(remainingTime))
                Thread.sleep(remainingTime.inWholeMilliseconds)
            }
        }
    }

    fun stop() {
        running = false
    }

    fun start() {
        executorService.execute(this)
    }

    fun processPendingRequests(): Result<Int, RequestProcessingFailure> = transactor.performAsResult { postbox ->
        // TODO: define a limit for the number of requests to process
        // TODO: exclude requests marked as permanent failures
        // TODO: mark requests as "processing" to allow for multiple instances of this function to run concurrently?
        val pendingRequests = postbox.pendingRequests(batchSize)
        for (pending in pendingRequests) {
            processPendingRequest(postbox, pending)
        }
        pendingRequests.size
    }.mapFailure { RequestProcessingFailure(it.message.orEmpty()) }

    private fun processPendingRequest(
        postbox: Postbox, pending: Postbox.PendingRequest,
        successCriteria: (Response) -> Boolean = { it.status.successful }
    ): Result<Unit, RequestProcessingFailure> = target(pending.request).let { response ->
        if (successCriteria(response)) {
            postbox.markProcessed(pending.requestId, response)
                .mapFailure { RequestProcessingFailure(it.description) }
        } else {
            Failure(RequestProcessingFailure("response was not successful"))
        }
    }
}

data class RequestProcessingFailure(val reason: String)

sealed class ProcessingEvent : Event {
    data class BatchProcessingSucceeded(val batchSize: Int, val duration: Duration) : ProcessingEvent()
    data class BatchProcessingFailed(val cause: String) : ProcessingEvent()
    data class PollWait(val duration: Duration) : ProcessingEvent()
}
