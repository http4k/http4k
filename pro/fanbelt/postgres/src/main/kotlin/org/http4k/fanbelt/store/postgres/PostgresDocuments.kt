/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.exposed.DocumentDialect

/**
 * Document storage on Postgres, with row-level security on fanbelt.tenant. Writes lock their tenant; for another
 * WriteLocking, use PostgresDocuments(locking).
 */
object PostgresDocuments : DocumentDialect by Postgres(TenantLocking) {
    operator fun invoke(locking: WriteLocking): DocumentDialect = Postgres(locking)
}
