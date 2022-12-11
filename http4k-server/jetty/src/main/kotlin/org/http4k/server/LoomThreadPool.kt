package org.http4k.server

import org.eclipse.jetty.util.thread.ThreadPool
import java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.Long.Companion.MAX_VALUE

class LoomThreadPool : ThreadPool {
    private val executorService = newVirtualThreadPerTaskExecutor()

    @Throws(InterruptedException::class)
    override fun join() {
        executorService.awaitTermination(MAX_VALUE, NANOSECONDS)
    }

    override fun getThreads() = 1

    override fun getIdleThreads() = 1

    override fun isLowOnThreads() = false

    override fun execute(command: Runnable) {
        executorService.submit(command)
    }
}
