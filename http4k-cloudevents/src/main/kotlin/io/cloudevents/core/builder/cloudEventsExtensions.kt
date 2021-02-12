package io.cloudevents.core.builder

import org.http4k.core.ContentType
import org.http4k.core.Uri
import java.net.URI

@Suppress("UNCHECKED_CAST")
fun <T : CloudEventBuilder> T.withDataContentType(contentType: ContentType): T = withDataContentType(contentType.value) as T

@Suppress("UNCHECKED_CAST")
fun <T : CloudEventBuilder> T.withSource(uri: Uri): T = withSource(URI.create(uri.toString())) as T
