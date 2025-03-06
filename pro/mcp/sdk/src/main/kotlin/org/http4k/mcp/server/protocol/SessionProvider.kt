package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.protocol.SessionId
import java.util.UUID
import kotlin.random.Random

/**
 * Provides a session identifier for a given connection request. This can be used to allocate a particular session
 * which can be used to track the connection.
 */
interface SessionProvider {
    fun assign(connectRequest: Request): SessionId
    fun check(connectRequest: Request): Boolean

    companion object {
        /**
         * Provides a totally random session identifier.
         */
        fun Random(random: Random) =
            object : SessionProvider {
                override fun assign(connectRequest: Request) =
                    SessionId.of(UUID(random.nextLong(), random.nextLong()).toString())

                override fun check(connectRequest: Request) = true
            }
    }
}
