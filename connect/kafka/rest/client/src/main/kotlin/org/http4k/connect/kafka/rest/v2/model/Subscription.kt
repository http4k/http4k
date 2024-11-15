package org.http4k.connect.kafka.rest.v2.model

import org.http4k.connect.kafka.rest.model.Topic
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Subscription(val topics: List<Topic>)
