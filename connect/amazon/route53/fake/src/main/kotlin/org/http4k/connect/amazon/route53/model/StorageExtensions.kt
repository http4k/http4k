package org.http4k.connect.amazon.route53.model

import org.http4k.connect.storage.Storage

fun Storage<VPC>.save(hostedZoneId: HostedZoneId, vpc: VPC) =
    set("${hostedZoneId.value}/${vpc.vpcId}", vpc)

fun Storage<VPC>.forZone(hostedZoneId: HostedZoneId) = this
    .keySet(hostedZoneId.value)
    .mapNotNull { get(it) }

fun Storage<ResourceRecordSet>.forDomain(domain: String) = keySet()
    .filter { it.endsWith(".$domain") }
    .mapNotNull { get(it) }
