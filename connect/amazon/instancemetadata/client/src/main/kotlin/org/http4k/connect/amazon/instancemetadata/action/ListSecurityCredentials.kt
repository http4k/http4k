package org.http4k.connect.amazon.instancemetadata.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.Ec2ProfileName
import org.http4k.connect.amazon.instancemetadata.Ec2MetadataAction
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri

@Http4kConnectAction
class ListSecurityCredentials : Ec2MetadataAction<List<Ec2ProfileName>> {
    private val uri = Uri.of("/latest/meta-data/iam/security-credentials")

    override fun toRequest() = Request(Method.GET, uri)

    override fun toResult(response: Response): Result<List<Ec2ProfileName>, RemoteFailure> {
        return when (response.status) {
            Status.OK -> response.bodyString()
                .lines()
                .filter { it.trim().isNotEmpty() }
                .map { Ec2ProfileName.of(it) }
                .let { Success(it) }

            Status.CONNECTION_REFUSED -> Success(emptyList())  // not in EC2 environment
            else -> Failure(asRemoteFailure(response))
        }
    }
}
