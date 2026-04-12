package org.http4k.connect.amazon.ses.model

import jakarta.mail.Message
import org.http4k.connect.model.Base64Blob
import java.io.ByteArrayOutputStream

fun Base64Blob.Companion.of(message: Message) = ByteArrayOutputStream().use {
    message.writeTo(it)
    it.toByteArray()
}.let(Base64Blob::encode)
