package org.http4k.connect.amazon.instancemetadata.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.instancemetadata.Ec2MetadataAction
import org.http4k.connect.amazon.instancemetadata.model.InstanceType
import org.http4k.connect.amazon.instancemetadata.value
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri

@Http4kConnectAction
class GetInstanceType : Ec2MetadataAction<InstanceType> {
    private val uri = Uri.of("/latest/meta-data/instance-type")

    override fun toRequest() = Request(Method.GET, uri)

    override fun toResult(response: Response) = when (response.status) {
        Status.OK -> Success(response.value(InstanceType))
        else -> Failure(asRemoteFailure(response))
    }
}
