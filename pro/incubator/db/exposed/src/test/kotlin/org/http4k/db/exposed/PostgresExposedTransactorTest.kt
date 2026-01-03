package org.http4k.db.exposed

import org.http4k.db.TransactorContract
import org.http4k.db.exposed.testing.ExposedAccountRepository
import org.http4k.db.testing.createPostgresDataSource
import org.http4k.db.testing.initialisePostgres

class PostgresExposedTransactorTest : TransactorContract() {
    override val dataSource = createPostgresDataSource()
    override fun transactor() = ExposedTransactor(dataSource, { ExposedAccountRepository() })
    override fun prepareDb() = initialisePostgres(dataSource.connection)
}
