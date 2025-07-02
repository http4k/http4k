package org.http4k.connect.amazon.route53.model

import kotlin.random.Random

fun HostedZone.toXml() = """<HostedZone>
  <CallerReference>$callerReference</CallerReference>
  <Id>$id</Id>
  <Name>$name</Name>
  ${config?.toXml().orEmpty()}
  <ResourceRecordSetCount>$resourceRecordSetCount</ResourceRecordSetCount>
</HostedZone>"""

private const val hexAlphabet = "0123456789ABCDEF"
fun HostedZoneId.Companion.random(random: Random): HostedZoneId {
    return HostedZoneId.parse((1..8).map { hexAlphabet.random(random) }.joinToString(""))
}
