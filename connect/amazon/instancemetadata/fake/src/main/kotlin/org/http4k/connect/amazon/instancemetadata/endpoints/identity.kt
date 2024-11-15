package org.http4k.connect.amazon.instancemetadata.endpoints

import org.http4k.connect.amazon.instancemetadata.InstanceMetadata
import org.http4k.connect.amazon.instancemetadata.InstanceMetadataServiceMoshi
import org.http4k.connect.amazon.instancemetadata.model.IdentityDocument
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind

private val identityLens = InstanceMetadataServiceMoshi.autoBody<IdentityDocument>().toLens()

fun getIdentityDocument(metadata: InstanceMetadata) = "/latest/dynamic/instance-identity/document" bind GET to {
    Response(OK).with(identityLens of metadata.identity)
}

fun getPublicHostName(metadata: InstanceMetadata) = "/latest/meta-data/public-hostname" bind GET to {
    Response(OK).body(metadata.publicHostName.value)
}

fun getHostName(metadata: InstanceMetadata) = "/latest/meta-data/hostname" bind GET to {
    Response(OK).body(metadata.privateHostName.value)
}

fun getLocalHostName(metadata: InstanceMetadata) = "/latest/meta-data/local-hostname" bind GET to {
    Response(OK).body(metadata.privateHostName.value)
}

fun getLocalIpV4(metadata: InstanceMetadata) = "/latest/meta-data/local-ipv4" bind GET to {
    Response(OK).body(metadata.privateIp.value)
}

fun getPublicIpV4(metadata: InstanceMetadata) = "/latest/meta-data/public-ipv4" bind GET to {
    Response(OK).body(metadata.publicIp.value)
}

fun getAmiId(metadata: InstanceMetadata) = "/latest/meta-data/ami-id" bind GET to {
    Response(OK).body(metadata.imageId.value)
}

fun getInstanceId(metadata: InstanceMetadata) = "/latest/meta-data/instance-id" bind GET to {
    Response(OK).body(metadata.instanceId.value)
}

fun getInstanceType(metadata: InstanceMetadata) = "/latest/meta-data/instance-type" bind GET to {
    Response(OK).body(metadata.instanceType.value)
}
