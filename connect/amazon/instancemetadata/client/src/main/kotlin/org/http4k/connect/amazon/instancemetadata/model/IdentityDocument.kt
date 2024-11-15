package org.http4k.connect.amazon.instancemetadata.model

import org.http4k.connect.amazon.core.model.Region
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class IdentityDocument(
    val pendingTime: Instant,
    val accountId: String,
    val architecture: String,
    val imageId: ImageId,
    val instanceId: InstanceId,
    val instanceType: InstanceType,
    val privateIp: IpV4Address,
    val region: Region,
    val availabilityZone: String,
    val version: String
)
