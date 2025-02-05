package org.http4k.postbox.exposed

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.onFailure
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import org.http4k.postbox.Postbox
import org.http4k.postbox.PostboxError
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyFailed
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyProcessed
import org.http4k.postbox.PostboxError.RequestNotFound
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.booleanLiteral
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning

class ExposedPostbox(prefix: String) : Postbox {
    private val table = PostboxTable(prefix)

    override fun store(pending: Postbox.PendingRequest): Result<RequestProcessingStatus, PostboxError> =
        table.upsertReturning(
            returning = listOf(table.requestId, table.response, table.failed),
            onUpdateExclude = listOf(table.request)
        ) { row ->
            row[requestId] = pending.requestId.value
            row[request] = pending.request.toString()
        }.single().toStatus()

    override fun status(requestId: RequestId) =
        table.select(listOf(table.requestId, table.request, table.response, table.failed))
            .where { table.requestId eq requestId.value }
            .singleOrNull()
            ?.toStatus() ?: Failure(RequestNotFound)

    private fun ResultRow.toStatus() = when {
        this[table.failed] -> Success(RequestProcessingStatus.Failed(this[table.response]?.let(Response::parse)))
        this[table.response] != null ->
            Success(RequestProcessingStatus.Processed(Response.parse(this[table.response]!!)))

        else -> Success(RequestProcessingStatus.Pending)
    }

    override fun markProcessed(requestId: RequestId, response: Response) =
        status(requestId)
            .onFailure { return it }
            .let {
                when(it){
                    is RequestProcessingStatus.Pending -> markProcessedInternal(requestId, response)
                    is RequestProcessingStatus.Failed -> Failure(RequestAlreadyFailed)
                    is RequestProcessingStatus.Processed -> Failure(RequestAlreadyProcessed)
                }
            }

    private fun markProcessedInternal(
        requestId: RequestId,
        response: Response
    ): Result<Unit, PostboxError> {
        table.update(where = { table.requestId eq requestId.value }) { row ->
            row[table.response] = response.toString()
        }
        return Success(Unit)
    }

    override fun markFailed(requestId: RequestId, response: Response?): Result<Unit, PostboxError> =
        status(requestId)
            .onFailure { return it }
            .let {
                when (it) {
                    is RequestProcessingStatus.Failed -> markFailureInternal(requestId, it.response ?: response)
                    is RequestProcessingStatus.Pending -> markFailureInternal(requestId, response)
                    is RequestProcessingStatus.Processed -> Failure(RequestAlreadyProcessed)
                }
            }


    private fun markFailureInternal(
        requestId: RequestId,
        response: Response?
    ): Result<Unit, PostboxError> {
        table.update(where = { table.requestId eq requestId.value }) { row ->
            row[table.response] = response?.toString()
            row[table.failed] = true
        }
        return Success(Unit)
    }

    override fun pendingRequests(batchSize: Int) =
        table.select(listOf(table.requestId, table.request))
            .where(table.response.isNull() and not(table.failed eq booleanLiteral(true)))
            .orderBy(table.createdAt, ASC)
            .limit(batchSize)
            .map {
                Postbox.PendingRequest(
                    RequestId.of(it[table.requestId]),
                    Request.parse(it[table.request])
                )
            }
}
