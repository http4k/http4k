package org.http4k.mcp.model

import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxValue
import dev.forkhandles.values.minValue
import kotlin.random.Random

class McpMessageId private constructor(value: Long) : LongValue(value) {
    companion object : LongValueFactory<McpMessageId>(::McpMessageId, 1L.minValue.and(MAX_MCP_MESSAGE_ID.maxValue)) {
        fun random(random: Random = Random) = of(random.nextLong(1, MAX_MCP_MESSAGE_ID))
    }
}

/**
 * This is the maximum Integer value that can be represented precisely by raw JSON number when
 * Moshi deserializes it as a double. MCP servers seem to need a precise integer value for the
 * message ID, so we need to limit the range of the message ID to this value.
 */
private const val MAX_MCP_MESSAGE_ID = 9_007_199_254_740_991L
