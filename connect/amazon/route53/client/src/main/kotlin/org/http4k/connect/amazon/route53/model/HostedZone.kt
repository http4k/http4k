package org.http4k.connect.amazon.route53.model

data class HostedZone(
    val callerReference: String,
    val id: String,
    val name: String,
    val config: HostedZoneConfig?,
//    val linkedService:
    val resourceRecordSetCount: Long?
)
