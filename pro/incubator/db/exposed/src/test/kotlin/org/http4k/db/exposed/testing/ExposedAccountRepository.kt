package org.http4k.db.exposed.testing

import org.http4k.db.exposed.testing.Accounts.balance
import org.http4k.db.testing.AccountRepository
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Sum
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.case
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.times
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

object Accounts: Table("ACCOUNTS"){
    val name: Column<String> = varchar("name", 255)
    val balance: Column<Int> = integer("balance")

    override val primaryKey = PrimaryKey(name)
}

object AccountMovements: Table("ACCOUNT_MOVEMENTS") {
    val transactionId: Column<String> = varchar("transaction_id", 36)
    val account: Column<String> = varchar("account", 255)
    val amount: Column<Int> = integer("amount")
    val direction: Column<String> = varchar("direction", 6)
}

class ExposedAccountRepository: AccountRepository {

    override fun createAccount(name: String, initialBalance: Int) {
        Accounts.insert {
            it[Accounts.name] = name
            it[balance] = initialBalance
        }
    }

    override fun getBalance(name: String): Int =
        Accounts.select(balance).where({ Accounts.name eq name }).first()[Accounts.balance]

    override fun getBalanceFromMovements(name: String): Int {
        val check = AccountMovements.direction eq stringLiteral("CREDIT")
        val regular = AccountMovements.amount
        val inverted = AccountMovements.amount * intLiteral(-1)
        val conditional = Sum(case().When(check, regular).Else(inverted), IntegerColumnType())
        return AccountMovements.select(conditional)
            .where({AccountMovements.account eq name})
            .first()
            .get(conditional)!!
    }

    override fun adjustBalance(name: String, amount: Int) {
        Accounts.update({Accounts.name eq name})
            { it[balance] = balance + amount }
    }

    override fun recordMovement(
        transactionId: UUID,
        account: String,
        amount: Int,
        direction: AccountRepository.Direction
    ) {
        AccountMovements.insert {
            it[AccountMovements.transactionId] = transactionId.toString()
            it[AccountMovements.account] = account
            it[AccountMovements.amount] = amount
            it[AccountMovements.direction] = direction.name
        }
    }

    override fun failOperation() {
        AccountMovements.insert { it[AccountMovements.transactionId] = "fail" }
    }
}
