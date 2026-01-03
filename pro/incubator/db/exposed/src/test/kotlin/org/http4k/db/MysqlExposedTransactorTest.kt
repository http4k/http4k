package org.http4k.db

import org.http4k.db.testing.ExposedAccountRepository
import org.http4k.db.testing.createMysqlDataSource
import org.http4k.db.testing.setupDatabase

class MysqlExposedTransactorTest : TransactorContract() {
    override val dataSource = createMysqlDataSource("http4k")
    override fun transactor() = ExposedTransactor(dataSource, { ExposedAccountRepository() })
    override fun prepareDb() = setupDatabase(createMysqlDataSource())
}
