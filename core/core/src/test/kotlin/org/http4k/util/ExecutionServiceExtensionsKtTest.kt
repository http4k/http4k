package org.http4k.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.filter.ZipkinTraces
import org.http4k.filter.ZipkinTracesStorage.Companion.THREAD_LOCAL
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration.ofMillis
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

class ExecutionServiceExtensionsKtTest {

    private val latch = CountDownLatch(1)
    private val ref = AtomicReference<ZipkinTraces>()

    @Test
    fun `propagates traces`() {
        assertPropagation { execute(runnable(ref, latch)) }
        assertPropagation { submit(runnable(ref, latch)) }
        assertPropagation { submit(callable(ref, latch)) }
        assertPropagation { submit(runnable(ref, latch), Unit) }
        assertPropagation { invokeAll(listOf(callable(ref, latch))) }
        assertPropagation { invokeAny(listOf(callable(ref, latch))) }
        assertPropagation { invokeAll(listOf(callable(ref, latch)), 1, SECONDS) }
        assertPropagation { invokeAny(listOf(callable(ref, latch)), 1, SECONDS) }
    }

    @Test
    fun `invokeAll honours explicit timeout`() {
        val executor = Executors.newSingleThreadExecutor().withRequestTracing(THREAD_LOCAL)

        val futures = executor.invokeAll(listOf(blocking()), 100, MILLISECONDS)

        assertThat(futures.single().isCancelled, equalTo(true))
    }

    @Test
    fun `invokeAny honours explicit timeout`() {
        val executor = Executors.newSingleThreadExecutor().withRequestTracing(THREAD_LOCAL)

        assertThrows<TimeoutException> {
            executor.invokeAny(listOf(blocking()), 100, MILLISECONDS)
        }
    }

    @Test
    fun `untimed invokeAll applies default timeout`() {
        val executor = Executors.newSingleThreadExecutor()
            .withRequestTracing(THREAD_LOCAL, ofMillis(100))

        val futures = executor.invokeAll(listOf(blocking()))

        assertThat(futures.single().isCancelled, equalTo(true))
    }

    @Test
    fun `untimed invokeAny applies default timeout`() {
        val executor = Executors.newSingleThreadExecutor()
            .withRequestTracing(THREAD_LOCAL, ofMillis(100))

        assertThrows<TimeoutException> {
            executor.invokeAny(listOf(blocking()))
        }
    }

    private fun blocking(): Callable<Unit> = Callable {
        Thread.sleep(2000)
    }

    private fun assertPropagation(block: ExecutorService.() -> Unit) {
        val executor = Executors.newSingleThreadExecutor().withRequestTracing(THREAD_LOCAL)
        val initial = THREAD_LOCAL.forCurrentThread()
        executor.block()
        latch.await()

        assertThat(initial, equalTo(ref.get()))
    }

    private fun runnable(ref: AtomicReference<ZipkinTraces>, latch: CountDownLatch): Runnable = Runnable {
        ref.set(THREAD_LOCAL.forCurrentThread())
        latch.countDown()
    }

    private fun callable(ref: AtomicReference<ZipkinTraces>, latch: CountDownLatch): Callable<Unit> = Callable {
        ref.set(THREAD_LOCAL.forCurrentThread())
        latch.countDown()
    }
}
