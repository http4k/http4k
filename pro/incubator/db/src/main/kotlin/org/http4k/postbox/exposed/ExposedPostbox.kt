package org.http4k.postbox.exposed

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import org.http4k.postbox.Postbox
import org.http4k.postbox.PostboxError
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.exposed.ExposedPostbox.Companion.PostboxTable.createdAt
import org.http4k.postbox.exposed.ExposedPostbox.Companion.PostboxTable.request
import org.http4k.postbox.exposed.ExposedPostbox.Companion.PostboxTable.requestId
import org.http4k.postbox.exposed.ExposedPostbox.Companion.PostboxTable.response
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning
import java.time.Instant

class ExposedPostbox : Postbox {
    companion object {
        private val dbTimestampNow = object : CustomFunction<Instant>("now", JavaInstantColumnType()) {}

        object PostboxTable : Table("postbox") {
            val requestId: Column<String> = varchar("request_id", 36)
            val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(dbTimestampNow)
            val request: Column<String> = text("request")
            val response: Column<String?> = text("response").nullable()
            override val primaryKey = PrimaryKey(requestId, name = "request_id_pk")
        }
    }

    override fun store(pending: Postbox.PendingRequest): Result<RequestProcessingStatus, PostboxError> =
        PostboxTable.upsertReturning(
            returning = listOf(requestId, response),
            onUpdateExclude = listOf(request)
        ) { row ->
            row[requestId] = pending.requestId.value
            row[request] = pending.request.toString()
        }.single().let { row ->
            if (row[response] != null) {
                Success(RequestProcessingStatus.Processed(Response.parse(row[response]!!)))
            } else {
                Success(RequestProcessingStatus.Pending)
            }
        }

    override fun status(requestId: RequestId): Result<RequestProcessingStatus, PostboxError> {
        TODO("Not yet implemented")
    }

    override fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError> {
        PostboxTable.update(where = { PostboxTable.requestId eq requestId.value }) { row ->
            row[PostboxTable.response] = response.toString()
        }
        return Success(Unit)
    }

    override fun pendingRequests(batchSize: Int) =
        PostboxTable.select(listOf(requestId, request))
            .where(response.isNull())
            .orderBy(createdAt, ASC)
            .limit(batchSize)
            .map {
                Postbox.PendingRequest(
                    RequestId.of(it[requestId]),
                    Request.parse(it[request])
                )
            }
}
