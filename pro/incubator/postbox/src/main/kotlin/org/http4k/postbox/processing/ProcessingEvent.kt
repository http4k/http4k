/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import org.http4k.events.Event
import org.http4k.postbox.RequestId
import java.time.Duration

sealed class ProcessingEvent : Event {
    data class BatchProcessingSucceeded(val batchSize: Int, val duration: Duration) : ProcessingEvent()
    data class BatchProcessingFailed(val reason: String) : ProcessingEvent()
    data class RequestProcessingSucceeded(val requestId: RequestId) : ProcessingEvent()
    data class RequestScheduledForRetry(val requestId: RequestId, val attempts: Int, val retryIn: Duration) : ProcessingEvent()
    data class RequestMarkedDead(val requestId: RequestId, val attempts: Int, val reason: String) : ProcessingEvent()
    data class RequestProcessingFailed(
        val requestId: RequestId,
        val reason: RequestProcessingFailureReason,
        val detail: String
    ) : ProcessingEvent()
    data class PollWait(val duration: Duration) : ProcessingEvent()
    data class ShutdownTimedOut(val shutdownGracePeriod: Duration) : ProcessingEvent()
}

enum class RequestProcessingFailureReason {
    FAILED_TO_MARK_PROCESSED,
    FAILED_TO_SCHEDULE_RETRY,
    FAILED_TO_MARK_DEAD
}
