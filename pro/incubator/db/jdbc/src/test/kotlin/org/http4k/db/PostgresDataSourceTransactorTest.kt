package org.http4k.db

import org.http4k.db.testing.PlainSqlAccountRepository
import org.http4k.db.testing.createPostgresDataSource
import org.http4k.db.testing.initialisePostgres

class PostgresDataSourceTransactorTest : TransactorWithRetryContract() {
    override val dataSource = createPostgresDataSource()
    override fun transactor() = DataSourceTransactor(dataSource, ::PlainSqlAccountRepository)
    override fun prepareDb() = initialisePostgres(dataSource.connection)
}
