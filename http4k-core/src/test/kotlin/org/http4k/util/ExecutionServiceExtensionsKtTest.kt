package org.http4k.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.filter.ZipkinTraces
import org.http4k.filter.ZipkinTracesStorage.Companion.THREAD_LOCAL
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
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
