package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.exposed.DocumentSchema
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.format.AutoMarshallingJson
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Document storage on Postgres.
 */
object PostgresDocumentStorage {
    operator fun invoke(db: Database, json: AutoMarshallingJson<*>, locking: WriteLocking = TenantLocking): DocumentStorage =
        ExposedDocumentStorage(db, json, Postgres(locking))

    fun schema(name: TableName): DocumentSchema = Postgres(TenantLocking).schema(name)
}
