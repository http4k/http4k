package org.http4k.db.testing

import java.util.UUID

class InMemoryAccountRepository : AccountRepository {
    val accounts = mutableMapOf<String, Int>()
    val movements = mutableListOf<Movement>()
    
    override fun createAccount(name: String, initialBalance: Int) {
        accounts[name] = initialBalance;
    }

    override fun getBalance(name: String): Int = accounts[name] ?: 0

    override fun getBalanceFromMovements(name: String): Int = movements.filter { it.account == name }
        .sumOf {
            when (it.direction) {
                AccountRepository.Direction.CREDIT -> it.amount
                AccountRepository.Direction.DEBIT -> -it.amount
            }
        }

    override fun adjustBalance(name: String, amount: Int) {
        accounts[name] = (accounts[name] ?: 0) + amount
    }

    override fun recordMovement(
        transactionId: UUID,
        account: String,
        amount: Int,
        direction: AccountRepository.Direction
    ) {
        movements.add(Movement(transactionId, account, amount, direction))
    }

    override fun failOperation() {
        error("some exception");
    }
}

data class Movement(
    val transactionId: UUID,
    val account: String,
    val amount: Int,
    val direction: AccountRepository.Direction
)
