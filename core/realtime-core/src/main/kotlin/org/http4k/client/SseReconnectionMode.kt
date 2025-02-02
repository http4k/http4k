package org.http4k.client

import java.time.Duration

/**
 * Determines how the SSE client should behave when the connection is lost.
 */
interface SseReconnectionMode {

    fun doReconnect(): Boolean

    // If the connection is lost, the client will not attempt to reconnect.
    data object Disconnect : SseReconnectionMode {
        override fun doReconnect() = false
    }

    // If the connection is lost, the client will attempt to reconnect immediately.
    data object Immediate : SseReconnectionMode {
        override fun doReconnect() = true
    }

    // If the connection is lost, the client will attempt to reconnect after the specified delay.
    data class Delayed(val delay: Duration) : SseReconnectionMode {
        override fun doReconnect(): Boolean {
            Thread.sleep(delay)
            return true
        }
    }
}
