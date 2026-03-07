package org.http4k.postbox.storage.jdbc

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.time.TimeSource
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import org.http4k.postbox.Postbox
import org.http4k.postbox.Postbox.PendingRequest
import org.http4k.postbox.PostboxError
import org.http4k.postbox.PostboxError.Companion.RequestAlreadyProcessed
import org.http4k.postbox.PostboxError.Companion.RequestMarkedAsDead
import org.http4k.postbox.PostboxError.RequestNotFound
import org.http4k.postbox.RequestId
import org.http4k.postbox.RequestProcessingStatus
import org.http4k.postbox.RequestProcessingStatus.Dead
import org.http4k.postbox.RequestProcessingStatus.Pending
import org.http4k.postbox.RequestProcessingStatus.Processed
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

class JdbcPostbox(private val dataSource: DataSource, prefix: String, private val timeSource: TimeSource) : Postbox {

    private val table = "${prefix}_postbox"

    override fun store(requestId: RequestId, request: Request): Result<RequestProcessingStatus, PostboxError> {
        val now = timeSource()
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO $table (request_id, request, created_at, process_at, status, failures)
                VALUES (?, ?, ?, ?, 'PENDING', 0)
                ON CONFLICT (request_id) DO NOTHING
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, requestId.value)
                stmt.setString(2, request.toString())
                stmt.setTimestamp(3, Timestamp.from(now))
                stmt.setTimestamp(4, Timestamp.from(now))
                stmt.executeUpdate()
            }
            return status(requestId)
        }
    }

    override fun status(requestId: RequestId): Result<RequestProcessingStatus, PostboxError> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT request_id, response, status, process_at, failures FROM $table WHERE request_id = ?"
            ).use { stmt ->
                stmt.setString(1, requestId.value)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toStatus() else Failure(RequestNotFound)
                }
            }
        }
    }

    private fun ResultSet.toStatus(): Result<RequestProcessingStatus, PostboxError> =
        when (getString("status")) {
            "PENDING" -> Success(Pending(getInt("failures"), getTimestamp("process_at").toInstant()))
            "PROCESSED" -> Success(Processed(Response.parse(getString("response")!!)))
            "DEAD" -> Success(Dead(getString("response")?.let(Response::parse)))
            else -> Failure(RequestNotFound)
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

    override fun markFailed(requestId: RequestId, delayReprocessing: Duration, response: Response?): Result<Unit, PostboxError> =
        status(requestId)
            .onFailure { return it }
            .let {
                when (it) {
                    is Pending -> markFailedInternal(requestId, delayReprocessing, response, it.processAt)
                    is Dead -> Failure(RequestMarkedAsDead)
                    is Processed -> Failure(RequestAlreadyProcessed)
                }
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

    override fun pendingRequests(batchSize: Int, atTime: Instant): List<PendingRequest> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT request_id, request, process_at, failures FROM $table
                WHERE status = 'PENDING' AND process_at <= ?
                ORDER BY process_at ASC
                LIMIT ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(atTime))
                stmt.setInt(2, batchSize)
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<PendingRequest>()
                    while (rs.next()) {
                        results += PendingRequest(
                            RequestId.of(rs.getString("request_id")),
                            Request.parse(rs.getString("request")),
                            rs.getTimestamp("process_at").toInstant(),
                            rs.getInt("failures")
                        )
                    }
                    return results
                }
            }
        }
    }

    private fun markProcessedInternal(requestId: RequestId, response: Response): Result<Unit, PostboxError> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE $table SET response = ?, status = 'PROCESSED' WHERE request_id = ?"
            ).use { stmt ->
                stmt.setString(1, response.toString())
                stmt.setString(2, requestId.value)
                stmt.executeUpdate()
            }
        }
        return Success(Unit)
    }

    private fun markFailedInternal(
        requestId: RequestId,
        delayReprocessing: Duration,
        response: Response?,
        previousProcessAt: Instant
    ): Result<Unit, PostboxError> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE $table SET response = ?, process_at = ?, failures = failures + 1 WHERE request_id = ?"
            ).use { stmt ->
                stmt.setString(1, response?.toString())
                stmt.setTimestamp(2, Timestamp.from(previousProcessAt + delayReprocessing))
                stmt.setString(3, requestId.value)
                stmt.executeUpdate()
            }
        }
        return Success(Unit)
    }

    private fun markDeadInternal(requestId: RequestId, response: Response?): Result<Unit, PostboxError> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE $table SET response = ?, status = 'DEAD' WHERE request_id = ?"
            ).use { stmt ->
                stmt.setString(1, response?.toString())
                stmt.setString(2, requestId.value)
                stmt.executeUpdate()
            }
        }
        return Success(Unit)
    }
}
