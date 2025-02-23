package org.http4k.connect.amazon.ses.model

import java.io.ByteArrayOutputStream
import jakarta.mail.Message

fun RawMessageBase64.Companion.of(message: Message) = ByteArrayOutputStream().use {
    message.writeTo(it)
    it.toByteArray()
}.let(RawMessageBase64::encode)
