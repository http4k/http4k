package org.http4k.mcp.util

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService

/**
 * Interface for objects which can be started.
 */
interface Startable {
    fun start(executor: SimpleScheduler = SimpleSchedulerService(1))
}
