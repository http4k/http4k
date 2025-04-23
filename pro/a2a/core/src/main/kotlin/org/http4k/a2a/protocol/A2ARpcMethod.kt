package org.http4k.a2a.protocol

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class A2ARpcMethod private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<A2ARpcMethod>(::A2ARpcMethod)
}
