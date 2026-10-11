/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import dev.forkhandles.time.FixedTimeSource
import org.http4k.core.HttpHandler
import org.http4k.events.Events
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset.UTC

class PostboxProcessingTest : PostboxProcessingContract() {

    override val timeSource = FixedTimeSource()

    override fun runPolls(polls: Int, target: HttpHandler, events: Events) {
        PostboxProcessing(
            transactor,
            target,
            maxPollingTime = pollingTime,
            context = TestExecutionContext(timeSource, polls),
            events = events,
            backoffStrategy = { _, _ -> reprocessingDelay }
        ).start()
    }

    override fun processPendingRequests(target: HttpHandler, events: Events) {
        PostboxProcessing(
            transactor,
            target,
            context = TestExecutionContext(timeSource, 1),
            events = events,
            backoffStrategy = { _, _ -> reprocessingDelay }
        ).processPendingRequests { it.status.successful }
    }

    override fun startWithRealThreads(
        target: HttpHandler,
        maxPollingTime: Duration,
        shutdownGracePeriod: Duration,
        events: Events
    ): () -> Unit = PostboxProcessing(
        transactor,
        target,
        maxPollingTime = maxPollingTime,
        shutdownGracePeriod = shutdownGracePeriod,
        events = events,
        context = DefaultExecutionContext(shutdownGracePeriod, Clock.fixed(timeSource(), UTC))
    ).apply { start() }::stop
}
