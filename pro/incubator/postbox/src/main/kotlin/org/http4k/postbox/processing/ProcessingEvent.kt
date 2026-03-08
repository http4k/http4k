package org.http4k.postbox.processing

import org.http4k.events.Event
import org.http4k.postbox.RequestId
import java.time.Duration

sealed class ProcessingEvent : Event {
    data class BatchProcessingSucceeded(val batchSize: Int, val duration: Duration) : ProcessingEvent()
    data class BatchProcessingFailed(val reason: String) : ProcessingEvent()
    data class RequestProcessingSucceeded(val requestId: RequestId) : ProcessingEvent()
    data class RequestProcessingFailed(val reason: String) : ProcessingEvent()
    data class PollWait(val duration: Duration) : ProcessingEvent()
}
