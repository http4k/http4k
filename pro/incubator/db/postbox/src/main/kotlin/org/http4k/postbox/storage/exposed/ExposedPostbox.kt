package org.http4k.postbox.storage.exposed

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.time.TimeSource
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import org.http4k.postbox.Postbox
import org.http4k.postbox.PostboxError
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyProcessed
import org.http4k.postbox.PostboxError.Companion.RequestMarkedAsDead
import org.http4k.postbox.PostboxError.RequestNotFound
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.RequestProcessingStatus.Dead
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import org.http4k.postbox.storage.exposed.PostboxTable.Status.DEAD
import org.http4k.postbox.storage.exposed.PostboxTable.Status.PENDING
import org.http4k.postbox.storage.exposed.PostboxTable.Status.PROCESSED
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import java.time.Duration
import java.time.Instant

class ExposedPostbox(prefix: String, private val timeSource: TimeSource) : Postbox {
    private val table = PostboxTable(prefix)

    override fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError> =
        table.upsertReturning(
            returning = listOf(table.requestId, table.response, table.status, table.processAt, table.failures),
            onUpdateExclude = listOf(table.request, table.createdAt, table.processAt, table.status, table.failures)
        ) { row ->
            val now = timeSource()
            row[table.requestId] = requestId.value
            row[createdAt] = now
            row[processAt] = now
            row[status] = PENDING
            row[table.request] = request.toString()
        }.single().toStatus()

    override fun status(requestId: RequestId) =
        table.select(
            listOf(
                table.requestId,
                table.request,
                table.response,
                table.status,
                table.processAt,
                table.failures
            )
        )
            .where { table.requestId eq requestId.value }
            .singleOrNull()
            ?.toStatus() ?: Failure(RequestNotFound)

    private fun ResultRow.toStatus() = when (this[table.status]) {
        PENDING -> Success(Pending(this[table.failures], this[table.processAt]))
        PROCESSED -> Success(Processed(Response.parse(this[table.response]!!)))
        DEAD -> Success(Dead(this[table.response]?.let(Response::parse)))
    }

    override fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError> =
        status(requestId)
            .onFailure { return it }
            .let {
                when (it) {
                    is Pending -> markProcessedInternal(requestId, response)
                    is Dead -> Failure(RequestMarkedAsDead)
                    is Processed -> Failure(RequestAlreadyProcessed)
                }
            }

    override fun markFailed(
        requestId: RequestId,
        delayReprocessing: Duration,
        response: Response?
    ): Result<Unit, PostboxError> = status(requestId)
        .onFailure { return it }
        .let {
            when (it) {
                is Pending -> markFailedInternal(requestId, delayReprocessing, response, it.processAt)
                is Dead -> Failure(RequestMarkedAsDead)
                is Processed -> Failure(RequestAlreadyProcessed)
            }
        }

    private fun markFailedInternal(
        requestId: RequestId,
        delayReprocessing: Duration,
        response: Response?,
        previousProcessAt: Instant
    ): Result<Unit, PostboxError> {
        table.update(where = { table.requestId eq requestId.value }) { row ->
            row[table.response] = response.toString()
            row[table.processAt] = previousProcessAt + delayReprocessing
            row[table.failures] = table.failures + 1
        }
        return Success(Unit)
    }

    private fun markProcessedInternal(
        requestId: RequestId,
        response: Response
    ): Result<Unit, PostboxError> {
        table.update(where = { table.requestId eq requestId.value }) { row ->
            row[table.response] = response.toString()
            row[table.status] = PROCESSED
        }
        return Success(Unit)
    }

    override fun markDead(requestId: RequestId, response: Response?): Result<Unit, PostboxError> =
        status(requestId)
            .onFailure { return it }
            .let {
                when (it) {
                    is Dead -> markDeadInternal(requestId, it.response ?: response)
                    is Pending -> markDeadInternal(requestId, response)
                    is Processed -> Failure(RequestAlreadyProcessed)
                }
            }


    private fun markDeadInternal(
        requestId: RequestId,
        response: Response?
    ): Result<Unit, PostboxError> {
        table.update(where = { table.requestId eq requestId.value }) { row ->
            row[table.response] = response?.toString()
            row[table.status] = DEAD
        }
        return Success(Unit)
    }

    override fun pendingRequests(batchSize: Int, atTime: Instant) =
        table.select(listOf(table.requestId, table.request, table.processAt, table.failures))
            .where((table.status eq PENDING) and (table.processAt lessEq atTime))
            .orderBy(table.processAt, SortOrder.ASC)
            .limit(batchSize)
            .map {
                Postbox.PendingRequest(
                    RequestId.of(it[table.requestId]),
                    Request.parse(it[table.request]),
                    it[table.processAt],
                    it[table.failures]
                )
            }
}
