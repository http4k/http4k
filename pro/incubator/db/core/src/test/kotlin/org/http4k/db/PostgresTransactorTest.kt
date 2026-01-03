package org.http4k.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.db.testing.ExposedAccountRepository
import org.http4k.db.testing.PlainSqlAccountRepository
import org.opentest4j.TestAbortedException
import java.sql.Connection

class PostgresDataSourceTransactorTest : TransactorWithRetryContract() {
    override val dataSource = createDataSource()
    override fun transactor() = DataSourceTransactor(dataSource, ::PlainSqlAccountRepository)
    override fun prepareDb() = initialisePostgres(dataSource.connection)
}

class PostgresExposedTransactorTest : TransactorContract() {
    override val dataSource = createDataSource()
    override fun transactor() = ExposedTransactor(dataSource, { ExposedAccountRepository() })
    override fun prepareDb() = initialisePostgres(dataSource.connection)
}

fun createDataSource() = try {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
    })
} catch (e: Exception) {
    throw TestAbortedException("Postgres not available")
}

private fun initialisePostgres(connection: Connection) {
    connection.createStatement().use {
        it.execute(
            """DROP SCHEMA public CASCADE; 
                CREATE SCHEMA public; 
                GRANT ALL ON SCHEMA public TO postgres;
                GRANT ALL ON SCHEMA public TO public;
                
                create table ACCOUNTS(
                    name varchar(255) PRIMARY KEY,
                    balance integer default 0 not null
                );
                
                create table if not exists ACCOUNT_MOVEMENTS
                (
                    transaction_id varchar(36) not null,
                    account varchar(255) not null references ACCOUNTS(name),
                    amount integer check ( amount >= 0 ) not null,
                    direction varchar(6) not null check (direction in ('CREDIT', 'DEBIT'))
                )
              """
        )

        it.execute("insert into ACCOUNTS (name, balance) values ('DEPOSITS', 0);")
    }
}
