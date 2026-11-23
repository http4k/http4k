package org.http4k.fanbelt.infrastructure

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit.SECONDS

/**
 * A scheduler that runs each task on a virtual thread, as many at once as are due, so tasks blocked
 * on a slow socket or a busy store never hold up the rest.
 */
fun VirtualThreadScheduler(): ScheduledExecutorService =
    ScheduledThreadPoolExecutor(Int.MAX_VALUE, Thread.ofVirtual().factory()).apply {
        setKeepAliveTime(1, SECONDS)
        allowCoreThreadTimeOut(true)
        executeExistingDelayedTasksAfterShutdownPolicy = false
        removeOnCancelPolicy = true
    }
