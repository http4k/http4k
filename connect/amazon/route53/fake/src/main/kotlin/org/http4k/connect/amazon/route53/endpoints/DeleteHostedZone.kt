package org.http4k.connect.amazon.route53.endpoints

import org.http4k.connect.amazon.route53.hostedZoneIdLens
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.storage.Storage
import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.contentType
import java.time.Clock
import java.util.UUID

fun deleteHostedZone(hostedZones: Storage<StoredHostedZone>, clock: Clock) = { request: Request ->
    val hostedZoneId = hostedZoneIdLens(request)

    hostedZones -= hostedZoneId

    val result = ChangeInfo(
        id = UUID.randomUUID().toString(),
        status = ChangeInfo.Status.INSYNC,
        submittedAt = clock.instant(),
        comment = null
    )

    Response(Status.OK)
        .contentType(ContentType.APPLICATION_XML)
        .body("""<?xml version="1.0" encoding="UTF-8"?>
<DeleteHostedZoneResponse>${result.toXml()}</DeleteHostedZoneResponse>""")
}
