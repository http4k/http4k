package org.http4k.connect.amazon.instancemetadata.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.Ec2ProfileName
import org.http4k.connect.amazon.instancemetadata.Ec2MetadataAction
import org.http4k.connect.amazon.instancemetadata.InstanceMetadataServiceMoshi
import org.http4k.connect.amazon.instancemetadata.model.Ec2Credentials
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri

@Http4kConnectAction
data class GetSecurityCredentials(private val ec2ProfileName: Ec2ProfileName) : Ec2MetadataAction<Ec2Credentials> {

    private val uri = Uri.of("/latest/meta-data/iam/security-credentials/$ec2ProfileName")
    private val lens = InstanceMetadataServiceMoshi.autoBody<Ec2Credentials>().toLens()

    override fun toRequest() = Request(Method.GET, uri)

    override fun toResult(response: Response) = when (response.status) {
        Status.OK -> Success(lens(response))
        else -> Failure(asRemoteFailure(response))
    }
}
