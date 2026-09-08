/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.storage.jdbc

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
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
            "PROCESSING" -> Success(RequestProcessingStatus.Processing(getInt("failures"), getTimestamp("process_at").toInstant()))
            "PROCESSED" -> Success(Processed(Response.parse(getString("response")!!)))
            "DEAD" -> Success(Dead(getString("response")?.let(Response::parse)))
            else -> Failure(RequestNotFound)
        }

    override fun markProcessed(requestId: RequestId, response: Response): Result<Unit, PostboxError> =
        updateStatus(requestId, "PROCESSED", response.toString())

    override fun markFailed(requestId: RequestId, delayReprocessing: Duration, response: Response?): Result<Unit, PostboxError> =
        status(requestId)
            .onFailure { return it }
            .let {
                when (it) {
                    is Pending -> markFailedInternal(requestId, delayReprocessing, response, it.processAt)
                    is RequestProcessingStatus.Processing -> markFailedInternal(requestId, delayReprocessing, response, timeSource())
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
                    is Pending, is RequestProcessingStatus.Processing -> markDeadInternal(requestId, response)
                    is Processed -> Failure(RequestAlreadyProcessed)
                }
            }

    override fun pendingRequests(batchSize: Int, atTime: Instant): List<PendingRequest> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT request_id, request, process_at, failures FROM $table
                WHERE status = 'PENDING' AND process_at <= ?
                ORDER BY process_at ASC, request_id ASC
                LIMIT ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(atTime))
                stmt.setInt(2, batchSize)
                stmt.executeQuery().use { rs -> return rs.toPendingRequests() }
            }
        }
    }

    override fun claim(batchSize: Int, atTime: Instant, lease: Duration): List<PendingRequest> =
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                reclaimExpired(conn, atTime)
                val results = claimNextBatch(conn, batchSize, atTime, lease)
                conn.commit()
                results
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }

    private fun claimNextBatch(
        conn: java.sql.Connection,
        batchSize: Int,
        atTime: Instant,
        lease: Duration
    ): List<PendingRequest> = conn.prepareStatement(
        """
        UPDATE $table
        SET status = 'PROCESSING', process_at = ?
        WHERE request_id IN (
            SELECT request_id FROM $table
            WHERE status = 'PENDING' AND process_at <= ?
            ORDER BY process_at ASC, request_id ASC
            LIMIT ?
        )
        RETURNING request_id, request, process_at, failures
        """.trimIndent()
    ).use { stmt ->
        stmt.setTimestamp(1, Timestamp.from(atTime + lease))
        stmt.setTimestamp(2, Timestamp.from(atTime))
        stmt.setInt(3, batchSize)
        stmt.executeQuery().use { rs -> rs.toPendingRequests() }
    }

    private fun ResultSet.toPendingRequests(): List<PendingRequest> {
        val results = mutableListOf<PendingRequest>()
        while (next()) {
            results += PendingRequest(
                RequestId.of(getString("request_id")),
                Request.parse(getString("request")),
                getTimestamp("process_at").toInstant(),
                getInt("failures")
            )
        }
        return results
    }

    private fun reclaimExpired(conn: java.sql.Connection, atTime: Instant) {
        conn.prepareStatement(
            """
            UPDATE $table SET status = 'PENDING'
            WHERE status = 'PROCESSING' AND process_at <= ?
            """.trimIndent()
        ).use { stmt ->
            stmt.setTimestamp(1, Timestamp.from(atTime))
            stmt.executeUpdate()
        }
    }

    private fun markDeadInternal(requestId: RequestId, response: Response?): Result<Unit, PostboxError> {
        val updated = dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                UPDATE $table SET status = 'DEAD', response = ?
                WHERE request_id = ? AND status IN ('PENDING', 'PROCESSING', 'DEAD')
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, response?.toString())
                stmt.setString(2, requestId.value)
                stmt.executeUpdate()
            }
        }
        return when {
            updated > 0 -> Success(Unit)

            else -> status(requestId).flatMap {
                when (it) {
                    is Processed -> Failure(RequestAlreadyProcessed)
                    is Pending, is RequestProcessingStatus.Processing, is Dead -> Failure(RequestNotFound)
                }
            }
        }
    }

    private fun updateStatus(requestId: RequestId, newStatus: String, response: String?): Result<Unit, PostboxError> {
        val updated = dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                UPDATE $table SET status = ?, response = ? WHERE request_id = ? AND status IN ('PENDING', 'PROCESSING')
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, newStatus)
                stmt.setString(2, response)
                stmt.setString(3, requestId.value)
                stmt.executeUpdate()
            }
        }
        return when {
            updated > 0 -> Success(Unit)

            else -> status(requestId).flatMap {
                when (it) {
                    is RequestProcessingStatus.Processing, is Pending -> Failure(RequestNotFound)
                    is Dead -> Failure(RequestMarkedAsDead)
                    is Processed -> Failure(RequestAlreadyProcessed)
                }
            }
        }
    }

    private fun markFailedInternal(
        requestId: RequestId,
        delayReprocessing: Duration,
        response: Response?,
        previousProcessAt: Instant
    ): Result<Unit, PostboxError> {
        val updated = dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                UPDATE $table SET response = ?, process_at = ?, failures = failures + 1
                WHERE request_id = ? AND status IN ('PENDING', 'PROCESSING')
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, response?.toString())
                stmt.setTimestamp(2, Timestamp.from(previousProcessAt + delayReprocessing))
                stmt.setString(3, requestId.value)
                stmt.executeUpdate()
            }
        }
        return when {
            updated > 0 -> Success(Unit)

            else -> status(requestId).flatMap {
                when (it) {
                    is RequestProcessingStatus.Processing, is Pending -> Failure(RequestNotFound)
                    is Dead -> Failure(RequestMarkedAsDead)
                    is Processed -> Failure(RequestAlreadyProcessed)
                }
            }
        }
    }
}
