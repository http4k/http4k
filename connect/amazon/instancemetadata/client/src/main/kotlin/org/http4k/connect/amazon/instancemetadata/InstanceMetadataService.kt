package org.http4k.connect.amazon.instancemetadata

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface InstanceMetadataService {
    operator fun <R> invoke(action: Ec2MetadataAction<R>): Result<R, RemoteFailure>

    companion object
}
