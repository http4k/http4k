package org.http4k.connect.amazon.route53.model

import org.http4k.connect.storage.Storage

fun Storage<VpcConfig>.save(hostedZoneId: HostedZoneId, vpc: VpcConfig) =
    set("${hostedZoneId.value}/${vpc.vpcId}", vpc)

fun Storage<VpcConfig>.forZone(hostedZoneId: HostedZoneId) = this
    .keySet(hostedZoneId.value)
    .mapNotNull { get(it) }
