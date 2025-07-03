package org.http4k.connect.amazon.route53.action

import org.http4k.connect.amazon.route53.Route53
import org.http4k.connect.amazon.route53.model.Change
import org.http4k.connect.amazon.route53.model.HostedZoneConfig
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.HostedZoneName
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.VPC

// FIXME remove when ksp learns to behave

fun Route53.changeResourceRecordSets(hostedZoneId: HostedZoneId, changes: List<Change>) =
    invoke(ChangeResourceRecordSets(hostedZoneId, changes))

fun Route53.createHostedZone(
    name: HostedZoneName,
    callerReference: String ,
    delegationSetId: String? = null,
    hostedZoneConfig: HostedZoneConfig? = null,
    vpc: VPC? = null,
    ) = invoke(CreateHostedZone(
        name = name,
        callerReference = callerReference,
        delegationSetId = delegationSetId,
        hostedZoneConfig = hostedZoneConfig,
        vpc = vpc
    ))

fun Route53.getHostedZone(hostedZoneId: HostedZoneId) =
    invoke(GetHostedZone(hostedZoneId))

fun Route53.deleteHostedZone(hostedZoneId: HostedZoneId) =
    invoke(DeleteHostedZone(hostedZoneId))

fun Route53.listResourceRecordSets(
    hostedZoneId: HostedZoneId,
    startRecordIdentifier: String? = null,
    maxItems: Int? = null,
    name: String? = null,
    type: ResourceRecordSet.Type? = null
) = invoke(ListResourceRecordSets(
    hostedZoneId = hostedZoneId,
    startRecordIdentifier = startRecordIdentifier,
    maxItems = maxItems,
    name = name,
    type = type
))
