/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence.exposed

import org.http4k.fanbelt.store.TableName
import org.jetbrains.exposed.v1.core.Table

/**
 * The table presence is kept in, fanbelt_presence by default, which the app's migrations create: one row per connection
 * and topic, with its expiry and carried value.
 */
class PresenceTable(name: TableName = TableName.of("fanbelt_presence")) : Table(name.value) {
    val key = varchar("key", 255)
    val connection = varchar("connection", 64)
    val who = varchar("who", 255)
    val expiresAt = long("expires_at")
    val value = text("value").nullable()
    override val primaryKey = PrimaryKey(key, connection)
}
