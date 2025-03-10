package org.http4k.client

import java.time.Duration

/**
 * Determines how a client should behave when the realtime connection is lost.
 */
interface ReconnectionMode {

    fun doReconnect(): Boolean

    // If the connection is lost, the client will not attempt to reconnect.
    data object Disconnect : ReconnectionMode {
        override fun doReconnect() = false
    }

    // If the connection is lost, the client will attempt to reconnect immediately.
    data object Immediate : ReconnectionMode {
        override fun doReconnect() = true
    }

    // If the connection is lost, the client will attempt to reconnect after the specified delay.
    data class Delayed(val delay: Duration) : ReconnectionMode {
        override fun doReconnect(): Boolean {
            Thread.sleep(delay)
            return true
        }
    }
}
