package org.http4k.db.exposed

import org.http4k.db.TransactorContract
import org.http4k.db.exposed.testing.ExposedAccountRepository
import org.http4k.db.testing.createMysqlDataSource
import org.http4k.db.testing.setupDatabase

class MysqlExposedTransactorTest : TransactorContract() {
    override val dataSource = createMysqlDataSource("http4k")
    override fun transactor() = ExposedTransactor(dataSource, { ExposedAccountRepository() })
    override fun prepareDb() = setupDatabase(createMysqlDataSource())
}
