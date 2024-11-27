package org.http4k.connect.amazon.cloudfront.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudfront.CloudFrontAction
import org.http4k.connect.amazon.cloudfront.model.CallerReference
import org.http4k.connect.amazon.cloudfront.model.DistributionId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

@Http4kConnectAction
data class CreateInvalidation(
    val distributionId: DistributionId,
    val paths: List<String>,
    val quantity: Int,
    val callerRef: CallerReference = CallerReference.random()
) : CloudFrontAction<Unit> {
    constructor (
        distributionId: DistributionId,
        path: String,
    ) : this(distributionId, path, CallerReference.random())

    constructor (
        distributionId: DistributionId,
        path: String,
        callerRef: CallerReference
    ) : this(distributionId, listOf(path), 1, callerRef)

    override fun toRequest() = Request(POST, uri())
        .body("""
<?xml version="1.0" encoding="UTF-8"?>
<InvalidationBatch xmlns="http://cloudfront.amazonaws.com/doc/2020-05-31/">
    <CallerReference>${callerRef}</CallerReference>
    <Paths>
        <Quantity>$quantity</Quantity>
        <Items>
            """ +
            paths.joinToString("") { "<Path>$it</Path>" } +
            """
        </Items>
    </Paths>
</InvalidationBatch>
""")

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful || status.redirection -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("/2020-05-31/distribution/$distributionId/invalidation")
}
