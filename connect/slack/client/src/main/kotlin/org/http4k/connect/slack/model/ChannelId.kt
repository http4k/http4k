package org.http4k.connect.slack.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ChannelId private constructor(value: String) : StringValue(value), WithChannelId {
    companion object : NonBlankStringValueFactory<ChannelId>(::ChannelId)

    override val channelId = this
}

interface WithChannelId {
    val channelId: ChannelId
}


