package org.http4k.connect.kafka.rest.model

import dev.forkhandles.values.ZERO

data class TopicOffsetState(
    val next: Offset = Offset.ZERO,
    val committed: Offset? = null
) {
    fun next(nextOffset: Offset) = TopicOffsetState(nextOffset, committed)
    fun commitAt(lastOffset: Offset) = TopicOffsetState(lastOffset.inc(), lastOffset.inc())
}
