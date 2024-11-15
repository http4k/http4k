package org.http4k.connect.amazon.instancemetadata.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.instancemetadata.Ec2MetadataAction
import org.http4k.connect.amazon.instancemetadata.model.IpV4Address
import org.http4k.connect.amazon.instancemetadata.value
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri

@Http4kConnectAction
class GetPublicIpv4 : Ec2MetadataAction<IpV4Address> {
    private val uri = Uri.of("/latest/meta-data/public-ipv4")

    override fun toRequest() = Request(Method.GET, uri)

    override fun toResult(response: Response) = when (response.status) {
        Status.OK -> Success(response.value(IpV4Address))
        else -> Failure(asRemoteFailure(response))
    }
}
