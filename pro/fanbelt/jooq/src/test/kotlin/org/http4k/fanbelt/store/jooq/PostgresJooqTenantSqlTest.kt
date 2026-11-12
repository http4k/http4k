/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.jooq

import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.fanbelt.store.exposed.PostgresDocuments
import org.http4k.fanbelt.store.exposed.postgresUrl
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database

class PostgresJooqTenantSqlTest : JooqTenantSqlContract() {
    override val storage by lazy {
        ExposedDocumentStorage(Database.connect(postgresUrl), Moshi, PostgresDocuments)
    }
}
