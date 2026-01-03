package org.http4k.db.testing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.db.Transactor
import org.http4k.db.testing.AccountRepository.Direction.CREDIT
import org.http4k.db.testing.AccountRepository.Direction.DEBIT
import org.http4k.events.EventFilters
import org.http4k.events.Events
import org.http4k.events.then
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.SECONDS

class UpdateInThread(
    private val transactor: Transactor<AccountRepository>, private val amount: Int,
    private val externalSemaphore: Semaphore,
    name: String,
    rawEvents: Events = {  }
) : Callable<Result<Unit, Exception>> {
    private val internalSemaphore = Semaphore(0)
    private var expectPause = true
    private var completed = false
    private val events = EventFilters.AddServiceName(name).then(EventFilters.AddTimestamp()).then(rawEvents)

    fun resume(waitOnRetry: Boolean = true) {
        if(!completed) {
            expectPause = waitOnRetry
            events(Event.Resuming)
            internalSemaphore.release()
        }
    }

    override fun call() = try {
        transactor.perform { repository ->
            events(Event.Starting)
            with(UUID.randomUUID()) {
                repository.recordMovement(this, "Alice", amount, DEBIT)
                repository.recordMovement(this, "Bob", amount, CREDIT)
            }
            if (expectPause) {
                events(Event.Pausing)
                externalSemaphore.release()
                events(Event.ReleasedExternal)
                if (!internalSemaphore.tryAcquire(5, SECONDS)) {
                    throw IllegalStateException("Failed to acquire internal semaphore")
                }
                events(Event.Resumed)
            }
            repository.adjustBalance("Alice", amount * -1)
            repository.adjustBalance("Bob", amount)
            events(Event.Completing)
            completed = true
            Success(Unit)
        }
    } catch (e: Exception) {
        Failure(e)
    }

    companion object {
        sealed class Event : org.http4k.events.Event {
            data object Starting : Event()
            data object Resuming : Event()
            data object Resumed : Event()
            data object Pausing : Event()
            data object ReleasedExternal : Event()
            data object Completing : Event()
        }
    }
}
