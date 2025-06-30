package org.http4k.connect.amazon.route53.endpoints

import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.storage.Storage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.time.Clock

fun createHostedZone(hostedZoned: Storage<StoredHostedZone>, clock: Clock) = { request: Request ->

    Response(Status.OK)
}
