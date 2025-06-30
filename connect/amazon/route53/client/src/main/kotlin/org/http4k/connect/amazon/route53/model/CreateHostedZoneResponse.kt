package org.http4k.connect.amazon.route53.model

data class CreateHostedZoneResponse(
    val changeInfo: ChangeInfo,
    val delegationSet: DelegationSet,
    val hostedZone: HostedZone?,
    val vpc: VpcConfig?
)
