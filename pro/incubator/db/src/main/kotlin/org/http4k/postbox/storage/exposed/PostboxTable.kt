package org.http4k.postbox.storage.exposed

import org.http4k.postbox.storage.exposed.PostboxTable.Status.PENDING
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

class PostboxTable(prefix: String) : Table("${prefix}_postbox") {
    val requestId: Column<String> = varchar("request_id", 36)
    val createdAt: Column<Instant> = timestamp("created_at")
    val processAt: Column<Instant> = timestamp("process_at")
    val request: Column<String> = text("request")
    val response: Column<String?> = text("response").nullable()
    val status = enumeration<Status>("status").default(PENDING)
    override val primaryKey = PrimaryKey(requestId, name = "${prefix}_request_id_pk")

    enum class Status {
        PENDING, PROCESSED, DEAD
    }
}
