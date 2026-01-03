package org.http4k.db

class MysqlExposedTransactorTest : TransactorContract() {
    override val dataSource = createDatasourceForDb("http4k")
    override fun transactor() = ExposedTransactor(dataSource, { ExposedAccountRepository() })
    override fun prepareDb() = setupDatabase(createDatasourceForDb())
}
