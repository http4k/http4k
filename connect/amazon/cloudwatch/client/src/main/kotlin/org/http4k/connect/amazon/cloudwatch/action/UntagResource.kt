package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.core.model.ARN
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class UntagResource(
    val ResourceARN: ARN,
    val TagKeys: List<String>,
) : CloudWatchAction<Unit>(Unit::class)
