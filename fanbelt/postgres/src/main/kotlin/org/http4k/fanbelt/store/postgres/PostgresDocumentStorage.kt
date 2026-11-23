/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.exposed.DocumentSchema
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.format.AutoMarshallingJson
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Document storage on Postgres, with row-level security on fanbelt.tenant. Writes lock their tenant, or as locking says
 * (KeyLocking). Reads go to db too: a replica would be read before it has the change a publish announces. Migrations
 * create each document table from schema(name).
 */
object PostgresDocumentStorage {
    operator fun invoke(db: Database, json: AutoMarshallingJson<*>, locking: WriteLocking = TenantLocking): DocumentStorage =
        ExposedDocumentStorage(db, json, Postgres(locking))

    fun schema(name: TableName): DocumentSchema = Postgres(TenantLocking).schema(name)
}
