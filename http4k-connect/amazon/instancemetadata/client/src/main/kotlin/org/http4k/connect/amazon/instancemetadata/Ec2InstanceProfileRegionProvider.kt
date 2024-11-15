package org.http4k.connect.amazon.instancemetadata

import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RegionProvider
import org.http4k.core.HttpHandler

/**
 * This provider will time out if not in an EC2 Environment.
 * For that reason, if there are multiple providers in a chain, this provider should be last.
 */
fun RegionProvider.Companion.Ec2InstanceProfile(ec2InstanceMetadata: InstanceMetadataService) =
    RegionProvider { ec2InstanceMetadata.getInstanceIdentityDocument().valueOrNull()?.region }

fun RegionProvider.Companion.Ec2InstanceProfile(http: HttpHandler = JavaHttpClient()) =
    Ec2InstanceProfile(InstanceMetadataService.Http(http))
