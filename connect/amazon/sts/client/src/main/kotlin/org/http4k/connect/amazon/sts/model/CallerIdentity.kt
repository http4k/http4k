package org.http4k.connect.amazon.sts.model

import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.ARN

data class CallerIdentity(
    val UserId: String,
    val Account: AwsAccount,
    val Arn: ARN
)
