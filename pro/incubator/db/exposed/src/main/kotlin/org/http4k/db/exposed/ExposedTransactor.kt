package org.http4k.db.exposed

import org.http4k.db.Transactor
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class ExposedTransactor<Resource>(
    dataSource: DataSource,
    private val createResource: (Database) -> Resource
): Transactor<Resource> {

    private val database: Database = Database.Companion.connect(dataSource)

    override fun <T> perform(mode: Transactor.Mode, work: (Resource) -> T): T =
        transaction(
            transactionIsolation = Database.Companion.getDefaultIsolationLevel(database),
            readOnly = mode == Transactor.Mode.ReadOnly
        ) {
            work(createResource(database))
        }
}
