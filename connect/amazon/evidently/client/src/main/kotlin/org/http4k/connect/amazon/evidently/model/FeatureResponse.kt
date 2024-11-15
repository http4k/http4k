package org.http4k.connect.amazon.evidently.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class FeatureResponse(
    val feature: Feature
)
