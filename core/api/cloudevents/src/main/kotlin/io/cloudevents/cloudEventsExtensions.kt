package io.cloudevents

import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.jackson.JsonCloudEventData
import org.http4k.format.Jackson.asJsonObject
import org.http4k.format.Jackson.defaultContentType

fun <T : CloudEvent> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this) { memo, next -> next(memo) }

fun <T : Any> CloudEventBuilder.withData(t: T) =
    withData(defaultContentType.value, JsonCloudEventData.wrap(
        asJsonObject(t)))
