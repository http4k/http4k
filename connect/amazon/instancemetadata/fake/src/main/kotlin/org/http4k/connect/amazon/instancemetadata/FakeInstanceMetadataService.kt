package org.http4k.connect.amazon.instancemetadata

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.instancemetadata.endpoints.getAmiId
import org.http4k.connect.amazon.instancemetadata.endpoints.getHostName
import org.http4k.connect.amazon.instancemetadata.endpoints.getIdentityDocument
import org.http4k.connect.amazon.instancemetadata.endpoints.getInstanceId
import org.http4k.connect.amazon.instancemetadata.endpoints.getInstanceType
import org.http4k.connect.amazon.instancemetadata.endpoints.getLocalHostName
import org.http4k.connect.amazon.instancemetadata.endpoints.getLocalIpV4
import org.http4k.connect.amazon.instancemetadata.endpoints.getPublicHostName
import org.http4k.connect.amazon.instancemetadata.endpoints.getPublicIpV4
import org.http4k.connect.amazon.instancemetadata.endpoints.getSecurityCredentials
import org.http4k.connect.amazon.instancemetadata.endpoints.listSecurityCredentials
import org.http4k.routing.routes
import java.time.Clock

class FakeInstanceMetadataService(
    clock: Clock = Clock.systemUTC(),
    private val metadata: InstanceMetadata = InstanceMetadata(clock.instant())
) : ChaoticHttpHandler() {

    override val app = routes(
        listSecurityCredentials(metadata),
        getSecurityCredentials(metadata, clock),
        getPublicHostName(metadata),
        getLocalHostName(metadata),
        getHostName(metadata),
        getPublicIpV4(metadata),
        getLocalIpV4(metadata),
        getIdentityDocument(metadata),
        getAmiId(metadata),
        getInstanceId(metadata),
        getInstanceType(metadata)
    )

    fun client() = InstanceMetadataService.Http(this)
}

fun main() {
    FakeInstanceMetadataService().start()
}
