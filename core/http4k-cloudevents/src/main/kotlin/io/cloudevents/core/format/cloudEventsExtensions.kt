package io.cloudevents.core.format

import org.http4k.core.ContentType

fun EventFormat.contentType() = ContentType(serializedContentType())
