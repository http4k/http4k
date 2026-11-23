package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.exposed.ExposedTenantSqlContract
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database

class PostgresTenantSqlTest : ExposedTenantSqlContract() {
    override val storage by lazy {
        PostgresDocumentStorage(Database.connect(postgresUrl), Moshi)
    }
}
