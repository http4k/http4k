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
import org.http4k.postbox.PostboxError.Companion.RequestMarkedAsDead
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyProcessed
import org.http4k.postbox.PostboxError.RequestNotFound
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.storage.exposed.PostboxTable.Status.DEAD
import org.http4k.postbox.storage.exposed.PostboxTable.Status.PENDING
import org.http4k.postbox.storage.exposed.PostboxTable.Status.PROCESSED
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning
import java.time.Duration
import java.time.Instant

class ExposedPostbox(prefix: String, private val timeSource: TimeSource) : Postbox {
    private val table = PostboxTable(prefix)

    override fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError> =
        table.upsertReturning(
            returning = listOf(table.requestId, table.response, table.status),
            onUpdateExclude = listOf(table.request, table.createdAt, table.processAt, table.status)
        ) { row ->
            val now = timeSource()
            row[table.requestId] = requestId.value
            row[createdAt] = now
            row[processAt] = now
            row[status] = PENDING
            row[table.request] = request.toString()
        }.single().toStatus()

    override fun status(requestId: RequestId) =
        table.select(listOf(table.requestId, table.request, table.response, table.status))
            .where { table.requestId eq requestId.value }
            .singleOrNull()
            ?.toStatus() ?: Failure(RequestNotFound)

    private fun ResultRow.toStatus() = when (this[table.status]) {
        PENDING -> Success(RequestProcessingStatus.Pending)
        PROCESSED -> Success(RequestProcessingStatus.Processed(Response.parse(this[table.response]!!)))
        DEAD -> Success(RequestProcessingStatus.Dead(this[table.response]?.let(Response::parse)))
    }

    override fun markProcessed(requestId: RequestId, response: Response) =
        status(requestId)
            .onFailure { return it }
            .let {
                when (it) {
                    is RequestProcessingStatus.Pending -> markProcessedInternal(requestId, response)
                    is RequestProcessingStatus.Dead -> Failure(RequestMarkedAsDead)
                    is RequestProcessingStatus.Processed -> Failure(RequestAlreadyProcessed)
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
                is RequestProcessingStatus.Pending -> markFailedInternal(requestId, delayReprocessing, response)
                is RequestProcessingStatus.Dead -> Failure(RequestMarkedAsDead)
                is RequestProcessingStatus.Processed -> Failure(RequestAlreadyProcessed)
            }
        }

    private fun markFailedInternal(
        requestId: RequestId,
        delayReprocessing: Duration,
        response: Response?
    ): Result<Unit, PostboxError> {
        table.update(where = { table.requestId eq requestId.value }) { row ->
            row[table.response] = response.toString()
            row[table.processAt] = timeSource() + delayReprocessing
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
                    is RequestProcessingStatus.Dead -> markDeadInternal(requestId, it.response ?: response)
                    is RequestProcessingStatus.Pending -> markDeadInternal(requestId, response)
                    is RequestProcessingStatus.Processed -> Failure(RequestAlreadyProcessed)
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
        table.select(listOf(table.requestId, table.request, table.processAt))
            .where(table.response.isNull() and (table.status eq PENDING) and (table.processAt lessEq atTime))
            .orderBy(table.processAt, ASC)
            .limit(batchSize)
            .map {
                Postbox.PendingRequest(
                    RequestId.of(it[table.requestId]),
                    Request.parse(it[table.request]),
                    it[table.processAt]
                )
            }
}
