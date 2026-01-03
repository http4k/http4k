package org.http4k.db.testing

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.opentest4j.TestAbortedException
import java.sql.Connection
import kotlin.use


fun createPostgresDataSource() = try {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
    })
} catch (e: Exception) {
    throw TestAbortedException("Postgres not available")
}

fun initialisePostgres(connection: Connection) {
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
