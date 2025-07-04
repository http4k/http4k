package org.http4k.connect.amazon.evidently

import org.http4k.connect.amazon.AwsRestJsonFake
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.ResourceId

fun AwsRestJsonFake.arn(resourceType: String, resourceId: ResourceId) =
    ARN.of(awsService, region, accountId, resourceType, resourceId)
