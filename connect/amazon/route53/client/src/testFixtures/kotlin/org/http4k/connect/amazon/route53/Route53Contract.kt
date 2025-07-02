package org.http4k.connect.amazon.route53

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.route53.action.ChangeResourceRecordSets
import org.http4k.connect.amazon.route53.action.CreateHostedZone
import org.http4k.connect.amazon.route53.action.DeleteHostedZone
import org.http4k.connect.amazon.route53.action.GetHostedZone
import org.http4k.connect.amazon.route53.action.ListResourceRecordSets
import org.http4k.connect.amazon.route53.model.AliasTarget
import org.http4k.connect.amazon.route53.model.Change
import org.http4k.connect.amazon.route53.model.HostedZone
import org.http4k.connect.amazon.route53.model.HostedZoneConfig
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.ListResourceRecordSetsResponse
import org.http4k.connect.amazon.route53.model.ResourceRecord
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.failureValue
import org.http4k.connect.successValue
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.junit.jupiter.api.Test
import java.util.UUID

interface Route53Contract: AwsContract {

    val route53 get() = Route53.Companion.Http({ aws.credentials }, http.debug())

    @Test
    fun `create and get hosted zone`() {
        val name = UUID.randomUUID().toString()
        val ref = UUID.randomUUID().toString()

        val result = route53(CreateHostedZone(
            name = name,
            callerReference = ref,
            delegationSetId = null,
            hostedZoneConfig = HostedZoneConfig(
                comment = "cool stuff",
                privateZone = true
            ),
            vpc = null // TODO test
        ))
            .successValue()

        try {
            assertThat(result.hostedZone.name, equalTo(name))
            assertThat(result.hostedZone.callerReference, equalTo(ref))
            assertThat(result.hostedZone.config?.privateZone, equalTo(true))
            assertThat(result.hostedZone.config?.comment, equalTo("cool stuff"))
            assertThat(result.hostedZone.resourceRecordSetCount, equalTo(0))

            val retrieved = route53(GetHostedZone(result.hostedZone.id)).successValue()
            assertThat(retrieved.hostedZone, equalTo(result.hostedZone))
            assertThat(retrieved.vpcs, hasSize(equalTo(0)))
        } finally {
            route53(DeleteHostedZone(result.hostedZone.id)).successValue()
        }
    }

    @Test
    fun `create resource record`() = route53.withHostedZone { zone ->
        route53(ChangeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(
                    action = Change.Action.CREATE,
                    resourceRecordSet = ResourceRecordSet(
                        name = "record.${zone.name}",
                        type = ResourceRecordSet.Type.CNAME,
                        ttl = 600,
                        resourceRecords = listOf(ResourceRecord("target.dns")),
                        aliasTarget = null
                    )
                )
            )
        )).successValue()

        route53(GetHostedZone(zone.id)).successValue { retrieved ->
            assertThat(retrieved.hostedZone.resourceRecordSetCount, equalTo(1))
        }
    }

    @Test
    fun `list resource records - simple, success`() = route53.withHostedZone { zone ->
            val records = listOf(
                ResourceRecordSet(
                    name = "record1.${zone.name}",
                    type = ResourceRecordSet.Type.CNAME,
                    ttl = 600,
                    resourceRecords = listOf(ResourceRecord("target.dns")),
                    aliasTarget = null
                ),
                ResourceRecordSet(
                    name = "record2.${zone.name}",
                    type = ResourceRecordSet.Type.A,
                    ttl = null,
                    resourceRecords = null,
                    aliasTarget = AliasTarget(
                        hostedZoneId = HostedZoneId.of("Z39HKRRJQ8AJ0"),
                        evaluateTargetHealth = true,
                        dnsName = "target.com"
                    )
                )
            )

            route53(ChangeResourceRecordSets(
                hostedZoneId = zone.id,
                changes = records.map { Change(Change.Action.CREATE, it) }
            )).successValue()

            assertThat(
                route53(ListResourceRecordSets(zone.id, null, null, null, null)).successValue(),
                equalTo(ListResourceRecordSetsResponse(
                    maxItems = "1000",
                    isTruncated = false,
                    nextRecordIdentifier = null,
                    nextRecordType = null,
                    nextRecordName = null,
                    resourceRecordSets = records
                ))
            )
        }

    @Test
    fun `list resource records - paginated, success`() = route53.withHostedZone { zone ->
        val recordSet1 = ResourceRecordSet(
            name = "record1.${zone.name}",
            type = ResourceRecordSet.Type.CNAME,
            ttl = 600,
            resourceRecords = listOf(ResourceRecord("target.dns")),
            aliasTarget = null
        )
        val recordSet2 = ResourceRecordSet(
            name = "record2.${zone.name}",
            type = ResourceRecordSet.Type.A,
            ttl = null,
            resourceRecords = null,
            aliasTarget = AliasTarget(
                hostedZoneId = HostedZoneId.of("Z39HKRRJQ8AJ0"),
                evaluateTargetHealth = true,
                dnsName = "target.com"
            )
        )

        route53(ChangeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.CREATE, recordSet1),
                Change(Change.Action.CREATE, recordSet2)
            )
        )).successValue()

        // page 1 of 2
        assertThat(
            route53(ListResourceRecordSets(zone.id, null, 1, null, null)).successValue(),
            equalTo(ListResourceRecordSetsResponse(
                maxItems = "1",
                isTruncated = true,
                nextRecordIdentifier = null,
                nextRecordType = recordSet2.type,
                nextRecordName = recordSet2.name,
                resourceRecordSets = listOf(recordSet1)
            ))
        )

        // page 2 of 2
        assertThat(
            route53(ListResourceRecordSets(zone.id, null, 1, recordSet2.name, null)).successValue(),
            equalTo(ListResourceRecordSetsResponse(
                maxItems = "1",
                isTruncated = false,
                nextRecordIdentifier = null,
                nextRecordType = null,
                nextRecordName = null,
                resourceRecordSets = listOf(recordSet2)
            ))
        )
    }

    @Test
    fun `delete resource record - success`() = route53.withHostedZone { zone ->
        val recordToDelete = ResourceRecordSet(
            name = "record2.${zone.name}",
            type = ResourceRecordSet.Type.CNAME,
            ttl = 600,
            resourceRecords = listOf(ResourceRecord("target.dns")),
            aliasTarget = null
        )

        route53.withCname(zone.id, "record1.${zone.name}", "target.dns")
        route53(ChangeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.CREATE, recordToDelete)
            )
        )).successValue()

        route53(ChangeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.DELETE, recordToDelete)
            )
        )).successValue()

        route53(GetHostedZone(zone.id)).successValue { retrieved ->
            assertThat(retrieved.hostedZone.resourceRecordSetCount, equalTo(1))
        }
        // TODO ensure proper record was deleted by getting resource records
    }

    @Test
    fun `delete resource record - zone not found`() {
        assertThat(
            route53(ChangeResourceRecordSets(
                hostedZoneId = HostedZoneId.of("RANDOM123"),
                changes = listOf(
                    Change(Change.Action.DELETE, ResourceRecordSet(
                        name = "record2.${UUID.randomUUID()}.com",
                        ttl = null,
                        resourceRecords = listOf(ResourceRecord("target.dns")),
                        type = ResourceRecordSet.Type.CNAME,
                        aliasTarget = null
                    ))
                )
            )).failureValue(),
        equalTo(RemoteFailure(
                method = Method.POST,
                uri = Uri.of("/2013-04-01/hostedzone/RANDOM123/rrset"),
                status = Status.NOT_FOUND,
                message = "NoSuchHostedZone"
            ))
        )
    }

    @Test
    fun `delete resource record - record not found`() = route53.withHostedZone { zone ->
        val recordToDelete = ResourceRecordSet(
            name = "record2.${zone.name}",
            type = ResourceRecordSet.Type.CNAME,
            ttl = 600,
            resourceRecords = listOf(ResourceRecord("target.dns")),
            aliasTarget = null
        )

        assertThat(
            route53(ChangeResourceRecordSets(
                hostedZoneId = zone.id,
                changes = listOf(
                    Change(Change.Action.DELETE, recordToDelete)
                )
            )).failureValue(),
            equalTo(RemoteFailure(
                method = Method.POST,
                uri = Uri.of("/2013-04-01/hostedzone/${zone.id}/rrset"),
                status = Status.BAD_REQUEST,
                message = "InvalidChangeBatch"
            ))
        )
    }

    @Test
    fun `delete resource record - wrong contents`() = route53.withHostedZone { zone ->
        val resourceName = "record1.${zone.name}"
        route53.withCname(zone.id, resourceName, "target.dns")

        assertThat(
            route53(ChangeResourceRecordSets(
                hostedZoneId = zone.id,
                changes = listOf(
                    Change(
                        action = Change.Action.DELETE,
                        resourceRecordSet = ResourceRecordSet(
                            name = resourceName,
                            type = ResourceRecordSet.Type.CNAME,
                            ttl = 600,
                            resourceRecords = listOf(ResourceRecord("wrong.dns")),
                            aliasTarget = null
                        )
                    )
                )
            )).failureValue(),
            equalTo(RemoteFailure(
                method = Method.POST,
                uri = Uri.of("/2013-04-01/hostedzone/${zone.id}/rrset"),
                status = Status.BAD_REQUEST,
                message = "InvalidChangeBatch"
            ))
        )
    }

    @Test
    fun `delete hosted zone - success`() {
        val hostedZone = route53(CreateHostedZone(
            name = UUID.randomUUID().toString(),
            callerReference = UUID.randomUUID().toString(),
            delegationSetId = null,
            hostedZoneConfig = null,
            vpc = null,
        )).successValue().hostedZone

        route53(DeleteHostedZone(hostedZone.id)).successValue()

        route53(GetHostedZone(hostedZone.id)).failureValue {
            assertThat(it.status, equalTo(Status.NOT_FOUND))
        }
    }

    @Test
    fun `delete hosted zone - not found`() {
        assertThat(
            route53(DeleteHostedZone(HostedZoneId.parse("RANDOM123"))).failureValue(),
            equalTo(RemoteFailure(
                method = Method.DELETE,
                uri = Uri.of("/2013-04-01/hostedzone/RANDOM123"),
                status = Status.NOT_FOUND,
                message = "NoSuchHostedZone"
            ))
        )
    }
}

private fun Route53.withHostedZone(
    name: String = UUID.randomUUID().toString(),
    fn: (HostedZone) -> Unit
) {
    val result = invoke(CreateHostedZone(
        name = name,
        callerReference = UUID.randomUUID().toString(),
        delegationSetId = null,
        hostedZoneConfig = null,
        vpc = null,
    )).successValue()

    try {
        fn(result.hostedZone)
    } finally {
        invoke(DeleteHostedZone(result.hostedZone.id)).successValue()
    }
}

private fun Route53.withCname(id: HostedZoneId, name: String, vararg targets: String) = invoke(ChangeResourceRecordSets(
    hostedZoneId = id,
    changes = listOf(
        Change(
            action = Change.Action.CREATE,
            resourceRecordSet = ResourceRecordSet(
                name = name,
                type = ResourceRecordSet.Type.CNAME,
                ttl = 600,
                resourceRecords = targets.map(::ResourceRecord),
                aliasTarget = null
            )
        )
    )
)).successValue()
