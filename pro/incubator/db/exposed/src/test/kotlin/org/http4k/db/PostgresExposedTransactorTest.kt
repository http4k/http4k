package org.http4k.db

import org.http4k.db.testing.ExposedAccountRepository
import org.http4k.db.testing.createPostgresDataSource
import org.http4k.db.testing.initialisePostgres

class PostgresExposedTransactorTest : TransactorContract() {
    override val dataSource = createPostgresDataSource()
    override fun transactor() = ExposedTransactor(dataSource, { ExposedAccountRepository() })
    override fun prepareDb() = initialisePostgres(dataSource.connection)
}
