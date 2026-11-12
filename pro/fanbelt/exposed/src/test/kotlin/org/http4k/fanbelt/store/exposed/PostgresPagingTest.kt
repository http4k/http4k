/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.PagingContract
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database

class PostgresPagingTest : PagingContract() {
    override val storage = ExposedDocumentStorage(provisioned(Database.connect(postgresUrl)), Moshi, PostgresDocuments)
}
