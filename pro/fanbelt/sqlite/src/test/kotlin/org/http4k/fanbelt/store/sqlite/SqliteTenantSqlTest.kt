/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.sqlite

import org.http4k.fanbelt.store.exposed.ExposedTenantSqlContract
import org.http4k.format.Moshi
import java.io.File
import java.nio.file.Files

class SqliteTenantSqlTest : ExposedTenantSqlContract() {
    override val storage = SqliteDocumentStorage(File(Files.createTempDirectory("hatch").toFile(), "hatch.db"), Moshi)
}
