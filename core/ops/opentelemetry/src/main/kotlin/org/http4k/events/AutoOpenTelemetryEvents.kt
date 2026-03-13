package org.http4k.events

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.context.Context
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.JsonType

fun <NODE : Any> AutoOpenTelemetryEvents(
    json: AutoMarshallingJson<NODE>,
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()
): Events {
    val logger = openTelemetry.logsBridge.get("http4k")

    return object : Events {
        override fun invoke(event: Event) {
            val unwrapped = when (event) {
                is MetadataEvent -> event.event
                else -> event
            }
            val metadata = when (event) {
                is MetadataEvent -> event.metadata
                else -> emptyMap()
            }

            val attrs = Attributes.builder()

            val jsonObj = json.asJsonObject(unwrapped)
            json.fields(jsonObj)
                .forEach { (key, value) -> addTypedAttribute(json, attrs, key, value) }

            metadata.forEach { (key, metaValue) -> attrs.put(AttributeKey.stringKey(key), metaValue.toString()) }

            logger.logRecordBuilder()
                .setContext(Context.current())
                .setBody(json.pretty(jsonObj))
                .setAllAttributes(attrs.build())
                .setSeverity(if (unwrapped is Event.Companion.Error) Severity.ERROR else Severity.INFO)
                .emit()
        }
    }
}

private fun <NODE : Any> addTypedAttribute(
    json: AutoMarshallingJson<NODE>,
    attrs: AttributesBuilder,
    key: String,
    value: NODE
) {
    when (json.typeOf(value)) {
        JsonType.String -> attrs.put(AttributeKey.stringKey(key), json.text(value))
        JsonType.Integer -> attrs.put(AttributeKey.longKey(key), json.integer(value))
        JsonType.Number -> attrs.put(AttributeKey.doubleKey(key), json.decimal(value).toDouble())
        JsonType.Boolean -> attrs.put(AttributeKey.booleanKey(key), json.bool(value))
        JsonType.Null -> {}
        JsonType.Object, JsonType.Array -> attrs.put(AttributeKey.stringKey(key), json.compact(value))
    }
}

