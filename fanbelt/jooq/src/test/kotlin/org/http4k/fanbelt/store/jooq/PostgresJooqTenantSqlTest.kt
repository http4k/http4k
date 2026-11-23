package org.http4k.fanbelt.store.jooq

import org.http4k.fanbelt.store.postgres.PostgresDocumentStorage
import org.http4k.fanbelt.store.postgres.postgresUrl
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database

class PostgresJooqTenantSqlTest : JooqTenantSqlContract() {
    override val storage by lazy {
        PostgresDocumentStorage(Database.connect(postgresUrl), Moshi)
    }
}
