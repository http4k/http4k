package org.http4k.connect.amazon.instancemetadata

import org.http4k.base64Encode
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Ec2ProfileName
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.amazon.instancemetadata.model.Ec2Credentials
import org.http4k.connect.amazon.instancemetadata.model.HostName
import org.http4k.connect.amazon.instancemetadata.model.IdentityDocument
import org.http4k.connect.amazon.instancemetadata.model.ImageId
import org.http4k.connect.amazon.instancemetadata.model.InstanceId
import org.http4k.connect.amazon.instancemetadata.model.InstanceType
import org.http4k.connect.amazon.instancemetadata.model.IpV4Address
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID

data class InstanceMetadata(
    val pendingTime: Instant,
    val accountId: String = "12345678",
    val imageId: ImageId = ImageId.of("ami-0123456789abcdef0"),
    val publicIp: IpV4Address = IpV4Address.of("1.2.3.4"),
    val privateIp: IpV4Address = IpV4Address.of("10.0.0.1"),
    val architecture: String = "arm64",
    val instanceType: InstanceType = InstanceType.of("t4g.small"),
    val instanceId: InstanceId = InstanceId.of("i-0123456789abcdef0"),
    val publicHostName: HostName = HostName.of("ip-${publicIp.value.replace(".", "-")}.ec2.internal"),
    val privateHostName: HostName = HostName.of("ip-${privateIp.value.replace(".", "-")}.ec2.internal"),
    val region: Region = Region.US_EAST_1,
    val availabilityZone: String = "${region.value.lowercase()}a",
    val profiles: Set<Ec2ProfileName> = setOf(Ec2ProfileName.of("default")),
    private val credentialsDuration: Duration = Duration.ofHours(1),
) {
    private var credentials = mutableMapOf<Ec2ProfileName, Ec2Credentials>()
    fun credentials() = credentials.toMap()

    fun getCredentials(profile: Ec2ProfileName, time: ZonedDateTime) = synchronized(credentials) fn@{
        if (profile !in profiles) return@fn null

        credentials[profile]
            ?.takeIf { it.Expiration.value > time }
            ?.let { return@fn it }

        return Ec2Credentials(
            Code = "Success",
            LastUpdated = time,
            Type = "AWS-HMAC",
            AccessKeyId = AccessKeyId.of(UUID.randomUUID().toString()),
            SecretAccessKey = SecretAccessKey.of(UUID.randomUUID().toString()),
            Token = SessionToken.of(UUID.randomUUID().toString().base64Encode()),
            Expiration = Expiration.of(time + credentialsDuration),
        ).also { credentials[profile] = it }
    }

    val identity
        get() = IdentityDocument(
            accountId = accountId,
            pendingTime = pendingTime,
            architecture = architecture,
            imageId = imageId,
            instanceId = instanceId,
            instanceType = instanceType,
            privateIp = privateIp,
            region = region,
            availabilityZone = availabilityZone,
            version = "2017-09-30"
        )
}
