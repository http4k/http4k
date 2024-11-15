package com.amazonaws.services.lambda.runtime.events;

import com.squareup.moshi.Json
import org.joda.time.DateTime

data class EventBridgeEvent(
    var source: String = "",
    @Json(name = "detail-type") var detailType: String = "",
    var detail: Map<String, Any> = mapOf(),
    var version: String? = null,
    var id: String? = null,
    var account: String? = null,
    var time: DateTime? = null,
    var region: String? = null,
    var resources: List<String>? = null
)
