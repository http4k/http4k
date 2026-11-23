package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.PagingContract
import org.http4k.fanbelt.store.PagingJson
import org.http4k.fanbelt.store.exposed.provisioned
import org.jetbrains.exposed.v1.jdbc.Database

class PostgresPagingTest : PagingContract() {
    override val storage = PostgresDocumentStorage(provisioned(Database.connect(postgresUrl), PostgresDocumentStorage::schema), PagingJson)
}
