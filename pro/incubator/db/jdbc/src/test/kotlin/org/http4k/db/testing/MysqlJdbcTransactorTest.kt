package org.http4k.db.testing

import org.http4k.db.DataSourceTransactor
import org.http4k.db.TransactorContract

class MysqlJdbcTransactorTest : TransactorContract() {
    override val dataSource = createDatasourceForDb("http4k")
    override fun transactor() = DataSourceTransactor(dataSource, ::PlainSqlAccountRepository)
    override fun prepareDb() = setupDatabase(createDatasourceForDb())
}
