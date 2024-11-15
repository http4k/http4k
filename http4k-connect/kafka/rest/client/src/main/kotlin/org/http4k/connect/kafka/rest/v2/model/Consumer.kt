package org.http4k.connect.kafka.rest.v2.model

import com.squareup.moshi.Json
import org.http4k.connect.kafka.rest.model.AutoCommitEnable
import org.http4k.connect.kafka.rest.model.AutoCommitEnable.`true`
import org.http4k.connect.kafka.rest.model.AutoOffsetReset
import org.http4k.connect.kafka.rest.model.AutoOffsetReset.latest
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.model.ConsumerRequestTimeout
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Consumer(
    val name: ConsumerInstance,
    val format: RecordFormat,
    @Json(name = "auto.offset.reset") val reset: AutoOffsetReset = latest,
    @Json(name = "auto.commit.enable") val enableAutocommit: AutoCommitEnable = `true`,
    @Json(name = "fetch.min.bytes") val minBytes: String? = null,
    @Json(name = "consumer.request.timeout.ms") val timeout: ConsumerRequestTimeout? = null
)

