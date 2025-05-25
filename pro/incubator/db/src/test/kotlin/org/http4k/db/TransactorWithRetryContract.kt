package org.http4k.db

import dev.forkhandles.result4k.Success
import org.http4k.db.testing.UpdateInThread
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.SECONDS

abstract class TransactorWithRetryContract: TransactorContract() {

    @Test
    open fun `retries serialisable transaction`() = runBlocking {
        val transactor = transactor()
        val executor = Executors.newVirtualThreadPerTaskExecutor()

        val testSemaphore = Semaphore(0)
        val updateOne = UpdateInThread(transactor, 3, testSemaphore, "one")
        val updateInThreadOne = executor.submit(updateOne).also { testSemaphore.tryAcquireOrFail() }

        val updateTwo = UpdateInThread(transactor, 7, testSemaphore, "two")
        val updateInThreadTwo = executor.submit(updateTwo).also { testSemaphore.tryAcquireOrFail() }

        updateTwo.resume(false)
        expectThat(updateInThreadTwo.get(2, SECONDS)).isEqualTo(Success(Unit))
        transactor.verifyBalance("Alice", 93)
        transactor.verifyBalance("Bob", 107)

        updateOne.resume(false)
        expectThat(updateInThreadOne.get(2, SECONDS)).isEqualTo(Success(Unit))
        transactor.verifyBalance("Alice", 90)
        transactor.verifyBalance("Bob", 110)
    }

    @Test
    open fun `throw if exceeds retries attempts`() = runBlocking {
        val transactor = transactor()
        val executor = Executors.newVirtualThreadPerTaskExecutor()

        val semaphore = Semaphore(0)
        val updaters = (0..3).map { UpdateInThread(transactor, 5, semaphore, "updater-$it") }
        val results = updaters.map { executor.submit(it).also { semaphore.tryAcquireOrFail() } }

        updaters[0].resume(false)
        expectThat(results[0].get(2, SECONDS)).isEqualTo(Success(Unit))
        (1..3).forEach { updaters[it].resume(true).also { semaphore.tryAcquireOrFail() } }

        updaters[1].resume(false)
        expectThat(results[1].get(2, SECONDS)).isEqualTo(Success(Unit))
        (2..3).forEach { updaters[it].resume(true).also { semaphore.tryAcquireOrFail() } }

        updaters[2].resume(false)
        expectThat(results[2].get(2, SECONDS)).isEqualTo(Success(Unit))

        updaters[3].resume(false)
        expectThat(results[3].get(2, SECONDS)).not().isEqualTo(Success(Unit))

        transactor.verifyBalance("Alice", 85)
        transactor.verifyBalance("Bob", 115)
    }

    private fun Semaphore.tryAcquireOrFail(permits: Int = 1) {
        if (!tryAcquire(permits, 1, SECONDS)) throw Exception("Failed to acquire $permits after 1 second")
    }
}
