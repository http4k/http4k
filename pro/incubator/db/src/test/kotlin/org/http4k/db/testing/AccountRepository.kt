package org.http4k.db.testing

import java.util.*

interface AccountRepository {
    enum class Direction { CREDIT, DEBIT }

    fun createAccount(name: String, initialBalance: Int)
    fun getBalance(name: String): Int
    fun getBalanceFromMovements(name: String): Int
    fun adjustBalance(name: String, amount: Int)
    fun recordMovement(transactionId: UUID, account: String, amount: Int, direction: Direction)
    fun failOperation()
}
