package org.http4k.connect.amazon.ses.model

import java.io.ByteArrayOutputStream
import jakarta.mail.Message
import org.http4k.connect.model.Base64Blob

fun Base64Blob.Companion.of(message: Message) = ByteArrayOutputStream().use {
    message.writeTo(it)
    it.toByteArray()
}.let(Base64Blob::encode)
