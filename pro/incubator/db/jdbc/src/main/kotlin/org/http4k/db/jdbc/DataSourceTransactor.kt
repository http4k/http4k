package org.http4k.db.jdbc

import org.http4k.db.Transactor
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

class DataSourceTransactor<Resource>(
    private val getConnection: () -> Connection,
    private val createResource: (Connection) -> Resource
) : Transactor<Resource> {
    constructor(dataSource: DataSource, createResource: (Connection) -> Resource) :
        this(dataSource::getConnection, createResource)

    override fun <T> perform(mode: Transactor.Mode, work: (Resource) -> T): T =
        getConnection().use { c ->
            c.autoCommit = false
            c.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE
            c.isReadOnly = (mode == Transactor.Mode.ReadOnly)

            c.runTransaction(createResource(c), work)
        }

    private fun <T> Connection.runTransaction(resource: Resource, work: (Resource) -> T): T {
        var tryCount = 0

        while (true) {
            tryCount++

            try {
                val result = work(resource)
                commit()
                return result
            } catch (e: Exception) {
                rollback()
                if (tryCount == 3 || !e.isSerialisationFailure()) {
                    throw e
                }
            }
        }
    }

    private fun Exception.isSerialisationFailure() = (this as? SQLException)?.sqlState == "40001"
}
