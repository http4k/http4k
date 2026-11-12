/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.sqlite

import org.http4k.fanbelt.store.PagingContract
import org.http4k.fanbelt.store.exposed.provisioned
import org.http4k.format.Moshi
import java.io.File
import java.nio.file.Files

class SqlitePagingTest : PagingContract() {
    private val file = File(Files.createTempDirectory("paging").toFile(), "paging.db").also { provisioned(SqliteDatabase(it), SqliteDocuments) }

    override val storage = SqliteDocumentStorage(file, Moshi)
}
