/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.format.MoshiNode
import org.http4k.format.unwrap
import org.http4k.format.wrap
import java.lang.reflect.Type

sealed class Part(
    open val metadata: Map<String, Any>?,
    open val filename: String?,
    open val mediaType: MimeType?
) {
    data class Text(
        val text: String,
        override val metadata: Map<String, Any>? = null,
        override val filename: String? = null,
        override val mediaType: MimeType? = null
    ) : Part(metadata, filename, mediaType)

    data class Raw(
        val raw: Base64Blob,
        override val metadata: Map<String, Any>? = null,
        override val filename: String? = null,
        override val mediaType: MimeType? = null
    ) : Part(metadata, filename, mediaType)

    data class Url(
        val url: Uri,
        override val metadata: Map<String, Any>? = null,
        override val filename: String? = null,
        override val mediaType: MimeType? = null
    ) : Part(metadata, filename, mediaType)

    data class Data(
        val data: MoshiNode,
        override val metadata: Map<String, Any>? = null,
        override val filename: String? = null,
        override val mediaType: MimeType? = null
    ) : Part(metadata, filename, mediaType)
}

object PartJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type != Part::class.java) return null
        return PartJsonAdapter(moshi)
    }
}

private class PartJsonAdapter(moshi: Moshi) : JsonAdapter<Part>() {
    private val mapAdapter = moshi.adapter<Map<String, Any>>(
        com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )
    private val nodeAdapter = moshi.adapter(Any::class.java)

    override fun fromJson(reader: JsonReader): Part {
        var text: String? = null
        var raw: String? = null
        var url: String? = null
        var data: Any? = null
        var metadata: Map<String, Any>? = null
        var filename: String? = null
        var mediaType: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "text" -> text = reader.nextString()
                "raw" -> raw = reader.nextString()
                "url" -> url = reader.nextString()
                "data" -> data = reader.readJsonValue()
                "metadata" -> metadata = mapAdapter.fromJson(reader)
                "filename" -> filename = reader.nextString()
                "mediaType" -> mediaType = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val mime = mediaType?.let { MimeType.of(it) }

        return when {
            text != null -> Part.Text(text, metadata, filename, mime)
            raw != null -> Part.Raw(Base64Blob.of(raw), metadata, filename, mime)
            url != null -> Part.Url(Uri.of(url), metadata, filename, mime)
            data != null -> Part.Data(MoshiNode.wrap(data), metadata, filename, mime)
            else -> error("Part has no content")
        }
    }

    override fun toJson(writer: JsonWriter, value: Part?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        when (value) {
            is Part.Text -> writer.name("text").value(value.text)
            is Part.Raw -> writer.name("raw").value(value.raw.value)
            is Part.Url -> writer.name("url").value(value.url.toString())
            is Part.Data -> {
                writer.name("data")
                nodeAdapter.toJson(writer, value.data.unwrap())
            }
        }
        value.metadata?.let { writer.name("metadata"); mapAdapter.toJson(writer, it) }
        value.filename?.let { writer.name("filename").value(it) }
        value.mediaType?.let { writer.name("mediaType").value(it.value) }
        writer.endObject()
    }
}
