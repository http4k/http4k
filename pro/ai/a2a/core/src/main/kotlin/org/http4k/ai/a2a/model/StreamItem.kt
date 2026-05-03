/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type

sealed interface StreamItem

object StreamItemJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type != StreamItem::class.java) return null
        return StreamItemJsonAdapter(moshi)
    }
}

private class StreamItemJsonAdapter(moshi: Moshi) : JsonAdapter<StreamItem>() {
    private val taskAdapter = moshi.adapter(Task::class.java)
    private val messageAdapter = moshi.adapter(Message::class.java)
    private val statusUpdateAdapter = moshi.adapter(TaskStatusUpdateEvent::class.java)
    private val artifactUpdateAdapter = moshi.adapter(TaskArtifactUpdateEvent::class.java)

    override fun fromJson(reader: JsonReader): StreamItem {
        val json = reader.readJsonValue() as Map<*, *>
        return when {
            json.containsKey("task") -> taskAdapter.fromJsonValue(json["task"])!!
            json.containsKey("message") -> messageAdapter.fromJsonValue(json["message"])!!
            json.containsKey("statusUpdate") -> statusUpdateAdapter.fromJsonValue(json["statusUpdate"])!!
            json.containsKey("artifactUpdate") -> artifactUpdateAdapter.fromJsonValue(json["artifactUpdate"])!!
            else -> error("StreamItem has no recognized payload")
        }
    }

    override fun toJson(writer: JsonWriter, value: StreamItem?) {
        if (value == null) { writer.nullValue(); return }
        writer.beginObject()
        when (value) {
            is Task -> { writer.name("task"); taskAdapter.toJson(writer, value) }
            is Message -> { writer.name("message"); messageAdapter.toJson(writer, value) }
            is TaskStatusUpdateEvent -> { writer.name("statusUpdate"); statusUpdateAdapter.toJson(writer, value) }
            is TaskArtifactUpdateEvent -> { writer.name("artifactUpdate"); artifactUpdateAdapter.toJson(writer, value) }
        }
        writer.endObject()
    }
}
