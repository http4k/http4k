/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.FixedTimeSource
import dev.forkhandles.tx.mem.InMemoryTransactor
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.postbox.Postbox
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.http4k.postbox.storage.inmemory.InMemoryPostbox
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class DefaultExecutionContextTest {

    private val postbox = InMemoryPostbox(FixedTimeSource())
    private val transactor = InMemoryTransactor<Postbox, Postbox>(postbox, { postbox })

    @Test
    fun `stop waits for an in-flight request to complete before returning`() {
        val requestId = RequestId.of("0")
        transactor.perform { it.store(requestId, Request(GET, "/slow")) }

        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val target = { _: Request ->
            started.countDown()
            release.await()
            Response(OK)
        }

        val processing = PostboxProcessing(transactor, target, context = DefaultExecutionContext())

        try {
            processing.start()
            assertThat(started.await(5, TimeUnit.SECONDS), equalTo(true))

            val stopped = CountDownLatch(1)
            thread {
                processing.stop()
                stopped.countDown()
            }

            assertThat(stopped.await(200, TimeUnit.MILLISECONDS), equalTo(false))

            release.countDown()

            assertThat(stopped.await(5, TimeUnit.SECONDS), equalTo(true))
            assertThat(
                transactor.perform { it.status(requestId) },
                equalTo(Success(Processed(Response(OK))))
            )
        } finally {
            release.countDown()
            processing.stop()
        }
    }
}
