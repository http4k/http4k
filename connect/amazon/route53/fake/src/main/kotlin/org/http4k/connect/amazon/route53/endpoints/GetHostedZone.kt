package org.http4k.connect.amazon.route53.endpoints

import org.http4k.connect.amazon.route53.hostedZoneIdLens
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.StoredResource
import org.http4k.connect.amazon.route53.model.VpcConfig
import org.http4k.connect.amazon.route53.model.forZone
import org.http4k.connect.amazon.route53.model.toHostedZone
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.storage.Storage
import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.contentType

fun getHostedZone(hostedZones: Storage<StoredHostedZone>, resources: Storage<StoredResource>, vpcs: Storage<VpcConfig>) = fn@{ request: Request ->
    val id = hostedZoneIdLens(request)
    val hostedZone = hostedZones[id.value] ?: return@fn Response(Status.NOT_FOUND)

    Response(Status.OK)
        .contentType(ContentType.APPLICATION_XML)
        .body("""<?xml version="1.0" encoding="UTF-8"?>
<GetHostedZoneResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
   ${hostedZone.toHostedZone(resources).toXml()}
   <VPCs>
   ${vpcs.forZone(hostedZone.id).joinToString("\n") { it.toXml() }}
   </VPCs>
</GetHostedZoneResponse>""")
}

