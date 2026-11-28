package org.http4k.fanbelt.presence.exposed

import org.http4k.fanbelt.store.TableName
import org.jetbrains.exposed.v1.core.Table

/**
 * The presence table.
 */
class PresenceTable(name: TableName = TableName.of("fanbelt_presence")) : Table(name.value) {
    val key = text("key")
    val connection = varchar("connection", 64)
    val who = text("who")
    val expiresAt = long("expires_at")
    val value = text("value").nullable()
    override val primaryKey = PrimaryKey(key, connection)
}
