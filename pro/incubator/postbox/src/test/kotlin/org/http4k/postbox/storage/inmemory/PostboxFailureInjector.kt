/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.storage.inmemory

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.postbox.Postbox
import org.http4k.postbox.Postbox.PendingRequest
import org.http4k.postbox.PostboxError
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import java.time.Duration
import java.time.Instant

/**
 * Test-only decorator that injects a failure into the next [store] call, with the underlying
 * [Postbox] remaining untouched. Useful to exercise error handling paths without polluting
 * the production implementation.
 */
class PostboxFailureInjector(private val delegate: Postbox) : Postbox {
    private var failNextStore = false

    fun failNext() {
        failNextStore = true
    }

    override fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError> =
        if (failNextStore) {
            failNextStore = false
            Failure(PostboxError.StorageFailure(IllegalStateException("Failed to store request")))
        } else {
            delegate.store(requestId, request)
        }

    override fun status(requestId: RequestId): Result<RequestProcessingStatus, PostboxError> = delegate.status(requestId)

    override fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError> =
        delegate.markProcessed(requestId, response)

    override fun markFailed(requestId: RequestId, delayReprocessing: Duration, response: Response?): Result<Unit, PostboxError> =
        delegate.markFailed(requestId, delayReprocessing, response)

    override fun markDead(requestId: RequestId, response: Response?): Result<Unit, PostboxError> =
        delegate.markDead(requestId, response)

    override fun pendingRequests(batchSize: Int, atTime: Instant): List<PendingRequest> =
        delegate.pendingRequests(batchSize, atTime)

    override fun claim(batchSize: Int, atTime: Instant, lease: Duration): List<PendingRequest> =
        delegate.claim(batchSize, atTime, lease)
}
