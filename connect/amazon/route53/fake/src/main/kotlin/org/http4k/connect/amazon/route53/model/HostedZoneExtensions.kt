package org.http4k.connect.amazon.route53.model

fun HostedZone.toXml() = """<HostedZone>
  <CallerReference>$callerReference</CallerReference>
  <Id>$id</Id>
  <Name>$name</Name>
  ${config?.toXml().orEmpty()}
  <ResourceRecordSetCount>$resourceRecordSetCount</ResourceRecordSetCount>
</HostedZone>"""
