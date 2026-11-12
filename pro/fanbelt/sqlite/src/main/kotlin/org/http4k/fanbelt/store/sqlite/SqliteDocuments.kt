/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.sqlite

import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.exposed.DocumentDialect
import org.http4k.fanbelt.store.exposed.DocumentSchema
import org.http4k.fanbelt.store.exposed.Sql
import org.http4k.fanbelt.tenancy.TenantId
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import org.sqlite.SQLiteErrorCode.SQLITE_BUSY
import org.sqlite.SQLiteException
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

object SqliteDocuments : DocumentDialect {
    private val schemas = ConcurrentHashMap<TableName, DocumentSchema>()

    override fun schema(name: TableName): DocumentSchema = schemas.computeIfAbsent(name) { DocumentSchema(it, { long(it) }, { text(it) }) }

    override fun text(doc: Column<String>, path: List<String>) = Sql("json_extract(", doc, ", '$.${path.joinToString(".")}')")

    override fun value(doc: Column<String>, path: List<String>) = text(doc, path)

    override fun nextSeq(docs: DocumentSchema, owned: Op<Boolean>) =
        (docs.select(docs.seq.max()).where(owned).single()[docs.seq.max()] ?: 0L) + 1

    override fun bind(connection: Connection, tenant: TenantId) = Unit

    override fun lock(connection: Connection, tenant: TenantId) = Unit

    override fun retryable(e: Exception) = e is SQLiteException && e.resultCode.code and 0xff == SQLITE_BUSY.code

    override fun read(db: Database) = write(db)
}
