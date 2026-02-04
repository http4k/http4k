package org.http4k.db.exposed

import org.http4k.db.Transactor
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import javax.sql.DataSource

class ExposedTransactor<Resource>(
    dataSource: DataSource,
    private val createResource: (Database) -> Resource
) : Transactor<Resource> {

    private val database: Database = Database.connect(dataSource)

    override fun <T> perform(mode: Transactor.Mode, work: (Resource) -> T): T =
        transaction(
            transactionIsolation = Database.getDefaultIsolationLevel(database),
            readOnly = mode == Transactor.Mode.ReadOnly
        ) {
            work(createResource(database))
        }
}
