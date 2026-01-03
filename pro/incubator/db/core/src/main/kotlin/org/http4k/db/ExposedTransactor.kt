package org.http4k.db

import org.http4k.db.Transactor.Mode.ReadOnly
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Database.Companion.connect
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class ExposedTransactor<Resource>(
    dataSource: DataSource,
    private val createResource: (Database) -> Resource
): Transactor<Resource> {

    private val database: Database = connect(dataSource)

    override fun <T> perform(mode: Transactor.Mode, work: (Resource) -> T): T =
        transaction(
            transactionIsolation = Database.getDefaultIsolationLevel(database),
            readOnly = mode == ReadOnly
        ) {
            work(createResource(database))
        }
}
