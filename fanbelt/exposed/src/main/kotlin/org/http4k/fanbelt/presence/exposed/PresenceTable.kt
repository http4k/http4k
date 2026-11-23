/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence.exposed

import org.http4k.fanbelt.store.TableName
import org.jetbrains.exposed.v1.core.Table

/**
 * The presence table, fanbelt_presence by default. The app's migrations create it.
 */
class PresenceTable(name: TableName = TableName.of("fanbelt_presence")) : Table(name.value) {
    val key = text("key")
    val connection = varchar("connection", 64)
    val who = text("who")
    val expiresAt = long("expires_at")
    val value = text("value").nullable()
    override val primaryKey = PrimaryKey(key, connection)
}
