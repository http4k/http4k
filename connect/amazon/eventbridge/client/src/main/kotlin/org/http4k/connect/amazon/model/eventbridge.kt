package org.http4k.connect.amazon.model

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.ResourceId
import kotlin.text.RegexOption.DOT_MATCHES_ALL as DOT_MATCHES_ALL1

class EventBusName private constructor(value: String) : ResourceId(value) {
    companion object : NonEmptyStringValueFactory<EventBusName>(::EventBusName) {
        fun of(arn: ARN) = of(arn.value)
    }
}

class Policy private constructor(value: String) : ResourceId(value) {
    companion object : NonEmptyStringValueFactory<Policy>(::Policy)
}

class EventSourceName private constructor(value: String) : StringValue(value) {
    companion object :
        StringValueFactory<EventSourceName>(::EventSourceName, "aws\\.partner(/[\\.\\-_A-Za-z0-9]+){2,}".regex)
}

class EventSource private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<EventSource>(::EventSource)
}

class EventDetail private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<EventDetail>(
        ::EventDetail,
        "\\{.*\\}".toRegex(DOT_MATCHES_ALL1).let { v -> { v.matches(it) } })
}

class EventDetailType private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<EventDetailType>(::EventDetailType)
}

class EndpointId private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<EndpointId>(::EndpointId)
}

class EventId private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<EventId>(::EventId)
}
