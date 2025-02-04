package org.http4k.postbox.exposed

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import org.http4k.postbox.Postbox
import org.http4k.postbox.PostboxError
import org.http4k.postbox.PostboxError.RequestNotFound
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning

class ExposedPostbox(prefix: String) : Postbox {
    private val table = PostboxTable(prefix)

    override fun store(pending: Postbox.PendingRequest): Result<RequestProcessingStatus, PostboxError> =
        table.upsertReturning(
            returning = listOf(table.requestId, table.response),
            onUpdateExclude = listOf(table.request)
        ) { row ->
            row[requestId] = pending.requestId.value
            row[request] = pending.request.toString()
        }.single().toStatus()

    override fun status(requestId: RequestId) =
        table.select(listOf(table.requestId, table.request, table.response))
            .where { table.requestId eq requestId.value }
            .singleOrNull()
            ?.toStatus() ?: Failure(RequestNotFound)

    private fun ResultRow.toStatus() = if (this[table.response] != null) {
        Success(RequestProcessingStatus.Processed(Response.parse(this[table.response]!!)))
    } else {
        Success(RequestProcessingStatus.Pending)
    }

    override fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError> {
        val update = table.update(where = { table.requestId eq requestId.value }) { row ->
            row[table.response] = response.toString()
        }
        return if (update == 0) Failure(RequestNotFound)
        else Success(Unit)
    }

    override fun pendingRequests(batchSize: Int) =
        table.select(listOf(table.requestId, table.request))
            .where(table.response.isNull())
            .orderBy(table.createdAt, ASC)
            .limit(batchSize)
            .map {
                Postbox.PendingRequest(
                    RequestId.of(it[table.requestId]),
                    Request.parse(it[table.request])
                )
            }
}
