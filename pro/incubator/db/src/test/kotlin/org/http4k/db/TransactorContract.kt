package org.http4k.db

import org.http4k.db.Transactor.Mode.ReadOnly
import org.http4k.db.testing.AccountRepository
import org.http4k.db.testing.AccountRepository.Direction.CREDIT
import org.http4k.db.testing.AccountRepository.Direction.DEBIT
import org.http4k.db.testing.PlainSqlAccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import java.util.*
import javax.sql.DataSource

abstract class TransactorContract {

    open fun prepareDb() = Unit
    abstract val dataSource: DataSource
    abstract fun transactor(): Transactor<AccountRepository>

    @BeforeEach
    fun prepareDbForTesting() {
        prepareDb()
        PlainSqlAccountRepository(dataSource.connection).createAccount("Alice", 100)
        PlainSqlAccountRepository(dataSource.connection).createAccount("Bob", 100)
    }

    @Test
    fun `perform regular write operation`() {
        val transactor = transactor()

        transactor.perform { repository ->
            with(UUID.randomUUID()) {
                repository.recordMovement(this, "Alice", 10, DEBIT)
                repository.recordMovement(this, "Bob", 10, CREDIT)
            }
            repository.adjustBalance("Alice", -10)
            repository.adjustBalance("Bob", 10)
        }

        transactor.verifyBalance("Alice", 90)
        transactor.verifyBalance("Bob", 110)
    }

    @Test
    fun `rollback on exception`() {
        val transactor = transactor()

        expectThrows<Exception> {
            transactor.perform { repository ->
                with(UUID.randomUUID()) {
                    repository.recordMovement(this, "Alice", 10, DEBIT)
                    repository.recordMovement(this, "Bob", 10, CREDIT)
                }
                repository.adjustBalance("Alice", -10)
                repository.failOperation()
                repository.adjustBalance("Bob", 10)
            }
        }

        transactor.verifyBalance("Alice", 100)
        transactor.verifyBalance("Bob", 100)
    }

    @Test
    fun `throw if read operation is performed in read-only transaction`() {
        val transactor = transactor()

        expectThrows<Exception> {
            transactor.perform(ReadOnly) { it.adjustBalance("Alice", 10) }
        }
    }

    @Test
    fun `throw if operation fails`() {
        val transactor = transactor()
        expectThrows<Exception> {
            transactor.perform { repository ->
                repository.failOperation()
            }
        }
    }

    fun Transactor<AccountRepository>.verifyBalance(
        account: String,
        expectedBalance: Int
    ) {
        expectThat(perform { it.getBalance(account) }).isEqualTo(expectedBalance)
        expectThat(perform { it.getBalanceFromMovements(account) }).isEqualTo(expectedBalance)
    }
}
