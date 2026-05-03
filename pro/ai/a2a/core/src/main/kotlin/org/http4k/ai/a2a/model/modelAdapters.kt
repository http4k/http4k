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

// --- Part ---

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
        if (value == null) { writer.nullValue(); return }
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

// --- StreamItem ---

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

// --- SecurityScheme ---

object SecuritySchemeJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type != SecurityScheme::class.java) return null
        return SecuritySchemeJsonAdapter(moshi)
    }
}

private class SecuritySchemeJsonAdapter(moshi: Moshi) : JsonAdapter<SecurityScheme>() {
    private val apiKeyAdapter = moshi.adapter(SecurityScheme.ApiKey::class.java)
    private val httpAuthAdapter = moshi.adapter(SecurityScheme.HttpAuth::class.java)
    private val oauth2Adapter = moshi.adapter(SecurityScheme.OAuth2::class.java)
    private val openIdConnectAdapter = moshi.adapter(SecurityScheme.OpenIdConnect::class.java)
    private val mutualTlsAdapter = moshi.adapter(SecurityScheme.MutualTls::class.java)

    override fun fromJson(reader: JsonReader): SecurityScheme {
        val json = reader.readJsonValue() as Map<*, *>
        return when {
            json.containsKey("apiKeySecurityScheme") -> apiKeyAdapter.fromJsonValue(json["apiKeySecurityScheme"])!!
            json.containsKey("httpAuthSecurityScheme") -> httpAuthAdapter.fromJsonValue(json["httpAuthSecurityScheme"])!!
            json.containsKey("oauth2SecurityScheme") -> oauth2Adapter.fromJsonValue(json["oauth2SecurityScheme"])!!
            json.containsKey("openIdConnectSecurityScheme") -> openIdConnectAdapter.fromJsonValue(json["openIdConnectSecurityScheme"])!!
            json.containsKey("mtlsSecurityScheme") -> mutualTlsAdapter.fromJsonValue(json["mtlsSecurityScheme"])!!
            else -> error("SecurityScheme has no recognized scheme key")
        }
    }

    override fun toJson(writer: JsonWriter, value: SecurityScheme?) {
        if (value == null) { writer.nullValue(); return }
        writer.beginObject()
        when (value) {
            is SecurityScheme.ApiKey -> { writer.name("apiKeySecurityScheme"); apiKeyAdapter.toJson(writer, value) }
            is SecurityScheme.HttpAuth -> { writer.name("httpAuthSecurityScheme"); httpAuthAdapter.toJson(writer, value) }
            is SecurityScheme.OAuth2 -> { writer.name("oauth2SecurityScheme"); oauth2Adapter.toJson(writer, value) }
            is SecurityScheme.OpenIdConnect -> { writer.name("openIdConnectSecurityScheme"); openIdConnectAdapter.toJson(writer, value) }
            is SecurityScheme.MutualTls -> { writer.name("mtlsSecurityScheme"); mutualTlsAdapter.toJson(writer, value) }
        }
        writer.endObject()
    }
}

// --- OAuthFlows ---

object OAuthFlowsJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type != OAuthFlows::class.java) return null
        return OAuthFlowsJsonAdapter(moshi)
    }
}

private class OAuthFlowsJsonAdapter(moshi: Moshi) : JsonAdapter<OAuthFlows>() {
    private val authCodeAdapter = moshi.adapter(OAuthFlows.AuthorizationCode::class.java)
    private val clientCredAdapter = moshi.adapter(OAuthFlows.ClientCredentials::class.java)
    private val deviceCodeAdapter = moshi.adapter(OAuthFlows.DeviceCode::class.java)

    override fun fromJson(reader: JsonReader): OAuthFlows {
        val json = reader.readJsonValue() as Map<*, *>
        return when {
            json.containsKey("authorizationCode") -> authCodeAdapter.fromJsonValue(json["authorizationCode"])!!
            json.containsKey("clientCredentials") -> clientCredAdapter.fromJsonValue(json["clientCredentials"])!!
            json.containsKey("deviceCode") -> deviceCodeAdapter.fromJsonValue(json["deviceCode"])!!
            else -> error("OAuthFlows has no recognized flow key")
        }
    }

    override fun toJson(writer: JsonWriter, value: OAuthFlows?) {
        if (value == null) { writer.nullValue(); return }
        writer.beginObject()
        when (value) {
            is OAuthFlows.AuthorizationCode -> { writer.name("authorizationCode"); authCodeAdapter.toJson(writer, value) }
            is OAuthFlows.ClientCredentials -> { writer.name("clientCredentials"); clientCredAdapter.toJson(writer, value) }
            is OAuthFlows.DeviceCode -> { writer.name("deviceCode"); deviceCodeAdapter.toJson(writer, value) }
        }
        writer.endObject()
    }
}
