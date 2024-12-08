package org.http4k.connect.amazon.evidently

import org.http4k.connect.amazon.AmazonRestJsonFake
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.ResourceId

fun AmazonRestJsonFake.arn(resourceType: String, resourceId: ResourceId) =
    ARN.of(awsService, region, accountId, resourceType, resourceId)
