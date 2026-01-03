package org.http4k.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.hsqldb.Server
import org.hsqldb.persist.HsqlProperties
import org.http4k.db.testing.PlainSqlAccountRepository
import org.junit.jupiter.api.AfterEach
import java.sql.Connection
import java.sql.DriverManager

class HSqlDataSourceTransactorTest : TransactorWithRetryContract() {

    private val server = HSqlServer().apply {
        start()
        setUpDbForTesting()
    }
    override val dataSource = server.dataSource()

    @AfterEach
    fun stopServer() {
        server.stop()
    }

    override fun transactor() = DataSourceTransactor(dataSource, ::PlainSqlAccountRepository)
}

class HSqlServer {
    private val server = Server().apply {
        setProperties(HsqlProperties().apply {
            setProperty("server.database.0", "mem:http4k-testing")
            setProperty("server.dbname.0", "http4k-testing")
        })
    }

    fun start() {
        server.start()
    }

    fun stop() {
        connection().createStatement().use {
            it.execute("TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK");
        }
        server.shutdown()
    }

    fun connection(): Connection {
        Class.forName("org.hsqldb.jdbcDriver")
        return DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/http4k-testing", "SA", "")
    }

     fun dataSource() = HikariDataSource(HikariConfig().apply {
        driverClassName = "org.hsqldb.jdbcDriver"
        username = "SA"
        password = ""
        jdbcUrl = "jdbc:hsqldb:hsql://localhost/http4k-testing"
    })
}

fun HSqlServer.setUpDbForTesting() {
    connection().createStatement().use {
        it.execute("""
            set database transaction control mvcc;
            set database transaction rollback on conflict true;
        
            create table if not exists ACCOUNTS
            (
                name longvarchar primary key,
                balance integer default 0 not null 
            );
            
            create table if not exists ACCOUNT_MOVEMENTS
            (
                transaction_id varchar(36) not null,
                account longvarchar not null references ACCOUNTS(name),
                amount integer check ( amount >= 0 ) not null,
                direction varchar(6) not null check (direction in ('CREDIT', 'DEBIT'))
            );
        """.trimIndent())

        it.execute("insert into ACCOUNTS (name, balance) values ('DEPOSITS', 0);")
    }
}
