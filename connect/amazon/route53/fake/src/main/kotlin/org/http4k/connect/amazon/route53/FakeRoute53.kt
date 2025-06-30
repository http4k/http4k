package org.http4k.connect.amazon.route53

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.route53.endpoints.changeResourceRecordSets
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.StoredResource
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock

class FakeRoute53(
    clock: Clock = Clock.systemUTC(),
    val hostedZones: Storage<StoredHostedZone> = Storage.InMemory(),
    val records: Storage<StoredResource> = Storage.InMemory()
) : ChaoticHttpHandler() {

    override val app = "/2013/04-01" bind routes(
        "hostedzone" bind routes(
            "/{hostedZoneId}" bind routes(
                "rrset" bind Method.POST to changeResourceRecordSets(hostedZones, records, clock)
            )
        )
    )

    /**
     * Convenience function to get a Route53 client
     */
    fun client() = Route53.Http({ AwsCredentials("accessKey", "secret") }, this)
}

fun main() {
    FakeRoute53().start()
}
