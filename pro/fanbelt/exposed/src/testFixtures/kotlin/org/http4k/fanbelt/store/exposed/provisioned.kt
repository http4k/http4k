/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.notes
import org.http4k.fanbelt.store.tagged
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun provisioned(db: Database, dialect: DocumentDialect = PostgresDocuments) = db.also { transaction(it) { SchemaUtils.create(dialect.schema(notes), dialect.schema(tagged)) } }
