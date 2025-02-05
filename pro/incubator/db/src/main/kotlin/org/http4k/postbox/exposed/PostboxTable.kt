package org.http4k.postbox.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

class PostboxTable(prefix: String) : Table("${prefix}_postbox") {
    private val dbTimestampNow = object : CustomFunction<Instant>("now", JavaInstantColumnType()) {}
    val requestId: Column<String> = varchar("request_id", 36)
    val createdAt: Column<Instant> = timestamp("created_at").defaultExpression(dbTimestampNow)
    val processAt: Column<Instant> = timestamp("process_at").defaultExpression(dbTimestampNow)
    val request: Column<String> = text("request")
    val response: Column<String?> = text("response").nullable()
    val dead = bool("dead").default(false)
    override val primaryKey = PrimaryKey(requestId, name = "${prefix}_request_id_pk")
}
