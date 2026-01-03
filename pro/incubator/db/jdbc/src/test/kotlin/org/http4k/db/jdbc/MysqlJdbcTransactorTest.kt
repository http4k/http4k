package org.http4k.db.jdbc

import org.http4k.db.TransactorContract
import org.http4k.db.testing.PlainSqlAccountRepository
import org.http4k.db.testing.createMysqlDataSource
import org.http4k.db.testing.setupDatabase

class MysqlJdbcTransactorTest : TransactorContract() {
    override val dataSource = createMysqlDataSource("http4k")
    override fun transactor() = DataSourceTransactor(dataSource, ::PlainSqlAccountRepository)
    override fun prepareDb() = setupDatabase(createMysqlDataSource())
}
