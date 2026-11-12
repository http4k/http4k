/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence.exposed

import org.http4k.fanbelt.presence.PresenceBackendContract
import org.http4k.fanbelt.store.exposed.postgresUrl
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedPresenceBackendTest : PresenceBackendContract() {

    override fun backend(): ExposedPresenceBackend {
        val db = Database.connect(postgresUrl)
        transaction(db) { SchemaUtils.create(PresenceTable()) }
        return ExposedPresenceBackend(db, clock)
    }
}
