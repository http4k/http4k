package org.http4k.connect.amazon.route53

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.route53.endpoints.changeResourceRecordSets
import org.http4k.connect.amazon.route53.endpoints.createHostedZone
import org.http4k.connect.amazon.route53.endpoints.deleteHostedZone
import org.http4k.connect.amazon.route53.endpoints.getHostedZone
import org.http4k.connect.amazon.route53.endpoints.listResourceRecordSets
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.VPC
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock
import kotlin.random.Random

internal val hostedZoneIdLens = Path.value(HostedZoneId).of("HostedZoneId")

class FakeRoute53(
    clock: Clock = Clock.systemUTC(),
    random: Random = Random,
    val hostedZones: Storage<StoredHostedZone> = Storage.InMemory(),
    val resources: Storage<ResourceRecordSet> = Storage.InMemory(),
    val vpcAssociations: Storage<VPC> = Storage.InMemory()
) : ChaoticHttpHandler() {

    override val app = "2013-04-01" bind routes(
        "hostedzone" bind routes(
            "" bind Method.POST to createHostedZone(random, hostedZones, resources, vpcAssociations, clock),
            "$hostedZoneIdLens" bind routes(
                "" bind Method.DELETE to deleteHostedZone(hostedZones, resources, clock),
                "" bind Method.GET to getHostedZone(hostedZones, resources, vpcAssociations),
                "rrset" bind Method.POST to changeResourceRecordSets(hostedZones, resources, clock),
                "rrset" bind Method.GET to listResourceRecordSets(hostedZones, resources)
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
