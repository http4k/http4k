package org.http4k.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.db.testing.ExposedAccountRepository
import org.http4k.db.testing.PlainSqlAccountRepository
import org.opentest4j.TestAbortedException
import javax.sql.DataSource

class MysqlDataSourceTransactorTest : TransactorContract() {
    override val dataSource = createDatasourceForDb("http4k")
    override fun transactor() = DataSourceTransactor(dataSource, ::PlainSqlAccountRepository)
    override fun prepareDb() = setupDatabase(createDatasourceForDb())
}

class MysqlExposedTransactorTest : TransactorContract() {
    override val dataSource = createDatasourceForDb("http4k")
    override fun transactor() = ExposedTransactor(dataSource, { ExposedAccountRepository() })
    override fun prepareDb() = setupDatabase(createDatasourceForDb())
}

fun createDatasourceForDb(database: String = "") = try {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        username = "root"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:mysql://localhost:3306/$database"
    })
} catch (e: Exception) {
    throw TestAbortedException("MySQL not available")
}

fun setupDatabase(dataSource: DataSource) {
    dataSource.connection.createStatement().use { statement ->
        """DROP DATABASE IF EXISTS http4k;
            CREATE DATABASE IF NOT EXISTS http4k;
            
            set global transaction isolation level serializable;
            
            USE http4k;
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
            );
            
            insert into ACCOUNTS (name, balance) values ('DEPOSITS', 0);
        """.trimIndent()
            .split(";")
            .map(String::trim)
            .filter(String::isNotBlank)
            .forEach(statement::execute)
    }
}
