package org.http4k.connect.amazon.route53

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.route53.endpoints.changeResourceRecordSets
import org.http4k.connect.amazon.route53.endpoints.createHostedZone
import org.http4k.connect.amazon.route53.endpoints.deleteHostedZone
import org.http4k.connect.amazon.route53.endpoints.getHostedZone
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.StoredResource
import org.http4k.connect.amazon.route53.model.VpcConfig
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock

internal val hostedZoneIdLens = Path.value(HostedZoneId).of("HostedZoneId")

class FakeRoute53(
    clock: Clock = Clock.systemUTC(),
    val hostedZones: Storage<StoredHostedZone> = Storage.InMemory(),
    val resources: Storage<StoredResource> = Storage.InMemory(),
    val vpcAssociations: Storage<VpcConfig> = Storage.InMemory()
) : ChaoticHttpHandler() {

    override val app = "/2013/04-01" bind routes(
        "hostedzone" bind routes(
            "" bind Method.POST to createHostedZone(hostedZones, resources, vpcAssociations, clock),
            "$hostedZoneIdLens" bind routes(
                "" bind Method.DELETE to deleteHostedZone(hostedZones, clock),
                "" bind Method.GET to getHostedZone(hostedZones, resources, vpcAssociations),
                "rrset" bind Method.POST to changeResourceRecordSets(hostedZones, resources, clock)
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
