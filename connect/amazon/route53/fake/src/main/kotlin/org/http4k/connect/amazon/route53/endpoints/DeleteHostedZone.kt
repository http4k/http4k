package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import org.http4k.connect.amazon.route53.hostedZoneIdLens
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.storage.Storage
import java.time.Clock
import java.util.UUID

fun deleteHostedZone(
    hostedZones: Storage<StoredHostedZone>,
    clock: Clock
) = route53FakeAction(
    successFn = { "<DeleteHostedZoneResponse>${it.toXml()}</DeleteHostedZoneResponse>" }
) fn@{
    val hostedZoneId = hostedZoneIdLens(this)
    hostedZones[hostedZoneId.value] ?: return@fn noSuchHostedZone().asFailure()
    hostedZones -= hostedZoneId.value

    ChangeInfo(
        id = UUID.randomUUID().toString(),
        status = ChangeInfo.Status.INSYNC,
        submittedAt = clock.instant(),
        comment = null
    ).asSuccess()
}
