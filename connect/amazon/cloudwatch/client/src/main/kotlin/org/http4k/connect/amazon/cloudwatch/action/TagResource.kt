package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Tag
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class TagResource(
    val ResourceARN: ARN,
    val Tags: List<Tag>,
) : CloudWatchAction<Unit>(Unit::class)
