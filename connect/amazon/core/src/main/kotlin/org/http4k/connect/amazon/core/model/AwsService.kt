package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.Uri

class AwsService private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AwsService>(::AwsService)

    fun toUri(region: Region) = Uri.of("https://$this.${region}.amazonaws.com")
}
