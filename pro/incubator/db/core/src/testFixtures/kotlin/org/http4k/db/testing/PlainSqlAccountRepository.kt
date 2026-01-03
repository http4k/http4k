package org.http4k.db.testing

import org.http4k.db.testing.AccountRepository.Direction
import org.http4k.db.testing.AccountRepository.Direction.*
import java.sql.Connection
import java.util.*
import kotlin.math.abs

class PlainSqlAccountRepository(private val connection: Connection) : AccountRepository {

    override fun createAccount(name: String, initialBalance: Int) {
        connection.prepareStatement("insert into ACCOUNTS (name, balance) values (?, ?)").use {
            it.setString(1, name)
            it.setInt(2, initialBalance)
            it.execute()
        }
        with(UUID.randomUUID()) {
            recordMovement(this, "DEPOSITS", initialBalance, DEBIT)
            recordMovement(this, name, initialBalance, CREDIT)
            adjustBalance("DEPOSITS", abs(initialBalance) * -1)
        }
    }

    override fun getBalance(name: String): Int {
        connection.prepareStatement("select balance from ACCOUNTS where name = ?").use {
            it.setString(1, name)
            it.executeQuery().use { result ->
                result.next()
                return result.getInt("balance")
            }
        }
    }

    override fun getBalanceFromMovements(name: String): Int {
        connection.prepareStatement(
            """
            select sum(case when direction = 'DEBIT' THEN amount * -1 else amount end) 
            from ACCOUNT_MOVEMENTS where account = ?
            """.trimIndent()
        ).use {
            it.setString(1, name)
            it.executeQuery().use { result ->
                result.next()
                return result.getInt(1)
            }
        }
    }

    override fun adjustBalance(name: String, amount: Int) {
        connection.prepareStatement("update ACCOUNTS set balance = balance + ? where name = ?").use {
            it.setInt(1, amount)
            it.setString(2, name)
            it.execute()
        }
    }

    override fun recordMovement(
        transactionId: UUID,
        account: String,
        amount: Int,
        direction: Direction
    ) {
        connection.prepareStatement(
            "insert into ACCOUNT_MOVEMENTS (transaction_id, account, amount, direction) values (?, ?, ?, ?)"
        ).use {
            it.setString(1, transactionId.toString())
            it.setString(2, account)
            it.setInt(3, amount)
            it.setString(4, direction.name)
            it.execute()
        }
    }

    override fun failOperation() {
        connection.prepareStatement("select something truly invalid").use {
            it.executeQuery()
        }
    }
}
