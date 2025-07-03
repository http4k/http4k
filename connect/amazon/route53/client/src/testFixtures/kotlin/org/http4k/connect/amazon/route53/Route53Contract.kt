package org.http4k.connect.amazon.route53

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.endsWith
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.hasSize
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.peek
import dev.forkhandles.result4k.recover
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.route53.action.changeResourceRecordSets
import org.http4k.connect.amazon.route53.action.createHostedZone
import org.http4k.connect.amazon.route53.action.deleteHostedZone
import org.http4k.connect.amazon.route53.action.getHostedZone
import org.http4k.connect.amazon.route53.action.listResourceRecordSets
import org.http4k.connect.amazon.route53.model.AliasTarget
import org.http4k.connect.amazon.route53.model.Change
import org.http4k.connect.amazon.route53.model.ErrorResponse
import org.http4k.connect.amazon.route53.model.HostedZone
import org.http4k.connect.amazon.route53.model.HostedZoneConfig
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.HostedZoneName
import org.http4k.connect.amazon.route53.model.ListResourceRecordSetsResponse
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.failureValue
import org.http4k.connect.successValue
import org.http4k.core.Body
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
        val name = HostedZoneName.parse("${UUID.randomUUID()}.com")
        val ref = UUID.randomUUID().toString()

        val result = route53.createHostedZone(
            name = name,
            callerReference = ref,
            hostedZoneConfig = HostedZoneConfig(
                comment = "cool stuff",
                privateZone = false
            ),
        ).successValue()

        try {
            assertThat(result.hostedZone.name, equalTo(name))
            assertThat(result.hostedZone.callerReference, equalTo(ref))
            assertThat(result.hostedZone.config?.privateZone, equalTo(false))
            assertThat(result.hostedZone.config?.comment, equalTo("cool stuff"))
            assertThat(result.hostedZone.resourceRecordSetCount, equalTo(2))

            val retrieved = route53.getHostedZone(result.hostedZone.id).successValue()
            assertThat(retrieved.hostedZone, equalTo(result.hostedZone))
            assertThat(retrieved.vpcs, hasSize(equalTo(0)))
        } finally {
            route53.deleteHostedZone(result.hostedZone.id).successValue()
        }
    }

    @Test
    fun `create private hosted zone - requires vpc`() {
        route53.createHostedZone(
            name = HostedZoneName.parse("${UUID.randomUUID()}.com"),
            callerReference = UUID.randomUUID().toString(),
            hostedZoneConfig = HostedZoneConfig(
                comment = "cool stuff",
                privateZone = true
            ),
        ).peek {
            // if hosted zone gets created (error-case), ensure it's deleted
            route53.deleteHostedZone(it.hostedZone.id)
        }.shouldBeFailure(
            Method.POST,
            Uri.of("/2013-04-01/hostedzone"),
            Status.BAD_REQUEST,
            ErrorResponse.Error(
                type = "Sender",
                code ="InvalidInput",
                message ="When you're creating a private hosted zone (when you specify true for PrivateZone), you must also specify values for VPCId and VPCRegion."
            )
        )
    }

    @Test
    fun `create resource record - already exists`() = route53.withHostedZone { zone ->
        val record = route53.withCname(zone.id, "record1.${zone.name}", "target1.dns")

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.CREATE, record)
            )
        ).shouldBeFailure(
            method = Method.POST,
            uri = Uri.of("/2013-04-01/hostedzone/${zone.id}/rrset"),
            status = Status.BAD_REQUEST,
            error = ErrorResponse.Error(
                type = "Sender",
                code ="InvalidChangeBatch",
                message ="[Tried to create resource record set [name='${record.name}', type='${record.type}'] but it already exists]"
            )
        )
    }

    @Test
    fun `create resource record`() = route53.withHostedZone { zone ->
        val expected = ResourceRecordSet(
            name = "record.${zone.name}",
            type = ResourceRecordSet.Type.CNAME,
            ttl = 600,
            resourceRecords = listOf("target.dns"),
            aliasTarget = null
        )

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.CREATE, expected)
            )
        ).successValue()

        assertThat(
            route53.listResourceRecordSets(zone.id).successValue().resourceRecordSets,
            hasElement(expected)
        )
    }

    @Test
    fun `create resource record - must match hosted zone name`() = route53.withHostedZone { zone ->
        val record = ResourceRecordSet(
            name = "record1.${UUID.randomUUID()}.com.",
            ttl = 600,
            resourceRecords = listOf("target.dns"),
            type = ResourceRecordSet.Type.CNAME,
            aliasTarget = null
        )

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.CREATE, record)
            )
        ).shouldBeFailure(
            method = Method.POST,
            uri = Uri.of("/2013-04-01/hostedzone/${zone.id}/rrset"),
            status = Status.BAD_REQUEST,
            error = ErrorResponse.Error(
                type = "Sender",
                code ="InvalidChangeBatch",
                message ="[RRSet with DNS name ${record.name} is not permitted in zone ${zone.name}]"
            )
        )
    }

    @Test
    fun `create resource record - normalizes with trailing dot`() = route53.withHostedZone { zone ->
        assertThat(zone.name.value, endsWith(".com."))
        val record = route53.withCname(zone.id, "record1.${zone.name.value.trimEnd('.')}", "target.dns")
        assertThat(record.name, endsWith(".com"))

        val created = route53.listResourceRecordSets(zone.id).successValue().resourceRecordSets
            .first { it.type == ResourceRecordSet.Type.CNAME }

        assertThat(created.name, equalTo("record1.${zone.name}"))
    }

    @Test
    fun `upsert resource record - does not exist`() = route53.withHostedZone { zone ->
        val record = ResourceRecordSet(
            name = "record1.${zone.name}",
            type = ResourceRecordSet.Type.CNAME,
            ttl = 600,
            resourceRecords = listOf("target.dns"),
            aliasTarget = null
        )

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.UPSERT, record)
            )
        ).successValue()

        assertThat(
            route53.listResourceRecordSets(zone.id).successValue().resourceRecordSets,
            hasElement(record)
        )
    }

    @Test
    fun `upsert resource record - existing`() = route53.withHostedZone { zone ->
        val original = route53.withCname(zone.id, "record1.${zone.name}", "target1.dns")
        val expected = original.copy(resourceRecords = listOf("target2.dns"))

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.UPSERT, expected)
            )
        ).successValue()

        val result = route53.listResourceRecordSets(zone.id).successValue().resourceRecordSets
        assertThat(result, hasElement(expected))
        assertThat(result, !hasElement(original))
    }

    @Test
    fun `list resource records - simple, success`() = route53.withHostedZone { zone ->
        val record1 = ResourceRecordSet(
            name = "record1.${zone.name}",
            type = ResourceRecordSet.Type.A,
            ttl = 600,
            resourceRecords = listOf("10.0.0.1"),
            aliasTarget = null
        )
        val record2 = ResourceRecordSet(
            name = "record2.${zone.name}",
            type = ResourceRecordSet.Type.A,
            ttl = null,
            resourceRecords = null,
            aliasTarget = AliasTarget(
                hostedZoneId = zone.id,
                evaluateTargetHealth = true,
                dnsName = record1.name
            )
        )

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.CREATE, record1),
                Change(Change.Action.CREATE, record2)
            )
        ).successValue()

        val result = route53.listResourceRecordSets(zone.id).successValue()

        assertThat(result.isTruncated, equalTo(false))
        assertThat(result.nextRecordName, absent())
        assertThat(result.nextRecordType, absent())
        assertThat(result.nextRecordIdentifier, absent())
        assertThat(result.resourceRecordSets, hasElement(record1))
        assertThat(result.resourceRecordSets, hasElement(record2))
    }

    @Test
    fun `list resource records - empty`() = route53.withHostedZone { zone ->
        assertThat(
            route53.listResourceRecordSets(zone.id, name = "foo.${zone.name}", type = ResourceRecordSet.Type.A).successValue().resourceRecordSets,
            equalTo(emptyList())
        )
    }

    @Test
    fun `list resource records - paginated, success`() = route53.withHostedZone { zone ->
        val recordSet1 = ResourceRecordSet(
            name = "record1.${zone.name}",
            type = ResourceRecordSet.Type.CNAME,
            ttl = 600,
            resourceRecords = listOf("target1.dns"),
            aliasTarget = null
        )
        val recordSet2 = ResourceRecordSet(
            name = "record2.${zone.name}",
            type = ResourceRecordSet.Type.CNAME,
            ttl = 600,
            resourceRecords = listOf("target2.dns"),
            aliasTarget = null
        )

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.CREATE, recordSet1),
                Change(Change.Action.CREATE, recordSet2)
            )
        ).successValue()

        // page 1 of 2
        assertThat(
            route53.listResourceRecordSets(
                hostedZoneId = zone.id,
                maxItems = 1,
                name = recordSet1.name,
                type = ResourceRecordSet.Type.CNAME
            ).successValue(),
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
            route53.listResourceRecordSets(
                hostedZoneId = zone.id,
                maxItems = 1,
                name = recordSet2.name,
                type = ResourceRecordSet.Type.CNAME
            ).successValue(),
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
        val recordToKeep = route53.withCname(zone.id, "record1.${zone.name}", "target.dns")
        val recordToDelete = route53.withCname(zone.id, "record2.${zone.name}", "target2.dns")

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.DELETE, recordToDelete)
            )
        ).successValue()

        val existingRecords = route53.listResourceRecordSets(zone.id).successValue().resourceRecordSets
        assertThat(existingRecords, hasElement(recordToKeep))
        assertThat(existingRecords, !hasElement(recordToDelete))
    }

    @Test
    fun `delete resource record - zone not found`() {
        route53.changeResourceRecordSets(
            hostedZoneId = HostedZoneId.of("RANDOM123"),
            changes = listOf(
                Change(Change.Action.DELETE, ResourceRecordSet(
                    name = "record2.${UUID.randomUUID()}.com",
                    ttl = null,
                    resourceRecords = listOf("target.dns"),
                    type = ResourceRecordSet.Type.CNAME,
                    aliasTarget = null
                ))
            )
        ).shouldBeFailure(
            method = Method.POST,
            uri = Uri.of("/2013-04-01/hostedzone/RANDOM123/rrset"),
            status = Status.NOT_FOUND,
            error = ErrorResponse.Error(
                type = "Sender",
                code = "NoSuchHostedZone",
                message = "No hosted zone found with ID: RANDOM123"
            )
        )
    }

    @Test
    fun `delete resource record - record not found`() = route53.withHostedZone { zone ->
        val recordToDelete = ResourceRecordSet(
            name = "record2.${zone.name}",
            type = ResourceRecordSet.Type.CNAME,
            ttl = 600,
            resourceRecords = listOf("target.dns"),
            aliasTarget = null
        )

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(Change.Action.DELETE, recordToDelete)
            )
        ).shouldBeFailure(
            method = Method.POST,
            uri = Uri.of("/2013-04-01/hostedzone/${zone.id}/rrset"),
            status = Status.BAD_REQUEST,
            error = ErrorResponse.Error(
                type = "Sender",
                code ="InvalidChangeBatch",
                message = "[Tried to delete resource record set [name='${recordToDelete.name}', type='${recordToDelete.type}'] but it was not found]"
            )
        )
    }

    @Test
    fun `delete resource record - wrong contents`() = route53.withHostedZone { zone ->
        val record = route53.withCname(zone.id, "record1.${zone.name}", "target.dns")

        route53.changeResourceRecordSets(
            hostedZoneId = zone.id,
            changes = listOf(
                Change(
                    action = Change.Action.DELETE,
                    resourceRecordSet = record.copy(type = ResourceRecordSet.Type.A)
                )
            )
        ).shouldBeFailure(
            method = Method.POST,
            uri = Uri.of("/2013-04-01/hostedzone/${zone.id}/rrset"),
            status = Status.BAD_REQUEST,
            error = ErrorResponse.Error(
                type = "Sender",
                code ="InvalidChangeBatch",
                message = "[Tried to delete resource record set [name='${record.name}', type='A'] but it was not found]"
            )
        )
    }

    @Test
    fun `delete hosted zone - success`() {
        val hostedZone = route53.createHostedZone(
            name = HostedZoneName.parse("${UUID.randomUUID()}.com"),
            callerReference = UUID.randomUUID().toString()
        ).successValue().hostedZone

        route53.deleteHostedZone(hostedZone.id).successValue()

        route53.getHostedZone(hostedZone.id).failureValue {
            assertThat(it.status, equalTo(Status.NOT_FOUND))
        }
    }

    @Test
    fun `delete hosted zone - not found`() {
        route53.deleteHostedZone(HostedZoneId.parse("RANDOM123")).shouldBeFailure(
            method = Method.DELETE,
            uri = Uri.of("/2013-04-01/hostedzone/RANDOM123"),
            status = Status.NOT_FOUND,
            error = ErrorResponse.Error(
                type = "Sender",
                code = "NoSuchHostedZone",
                message = "No hosted zone found with ID: RANDOM123"
        ))
    }

    @Test
    fun `delete hosted zone - not empty`() = route53.withHostedZone { zone ->
        route53.withCname(zone.id, "record1.${zone.name}", "target.dns")

        route53.deleteHostedZone(zone.id).shouldBeFailure(
            method = Method.DELETE,
            uri = Uri.of("/2013-04-01/hostedzone/${zone.id}"),
            status = Status.BAD_REQUEST,
            error = ErrorResponse.Error(
                type = "Sender",
                code = "HostedZoneNotEmpty",
                message = "The specified hosted zone contains non-required resource record sets and so cannot be deleted."
            )
        )
    }
}

private fun Route53.withHostedZone(
    name: HostedZoneName = HostedZoneName.parse("${UUID.randomUUID()}.com"),
    fn: (HostedZone) -> Unit
) {
    val result = createHostedZone(
        name = name,
        callerReference = UUID.randomUUID().toString(),
        delegationSetId = null,
        hostedZoneConfig = null,
        vpc = null,
    ).successValue()

    try {
        fn(result.hostedZone)
    } finally {
        val recordsToDelete = listResourceRecordSets(result.hostedZone.id)
            .map { it.resourceRecordSets }
            .recover { emptyList() }
            .filter { it.type != ResourceRecordSet.Type.NS && it.type != ResourceRecordSet.Type.SOA } // required records

        changeResourceRecordSets(
            hostedZoneId = result.hostedZone.id,
            changes = recordsToDelete.map { Change(Change.Action.DELETE, it) }
        )

        deleteHostedZone(result.hostedZone.id).successValue()
    }
}

private fun Route53.withCname(id: HostedZoneId, name: String, vararg targets: String): ResourceRecordSet {
    val resource = ResourceRecordSet(
        name = name,
        type = ResourceRecordSet.Type.CNAME,
        ttl = 600,
        resourceRecords = targets.toList(),
        aliasTarget = null
    )

    changeResourceRecordSets(
        hostedZoneId = id,
        changes = listOf(
            Change(Change.Action.CREATE, resource)
        )
    ).successValue()

    return resource
}

private fun Result4k<*, RemoteFailure>.shouldBeFailure(
    method: Method,
    uri: Uri,
    status: Status,
    error: ErrorResponse.Error?
) = failureValue {
    assertThat(it.method, equalTo(method))
    assertThat(it.uri, equalTo(uri))
    assertThat(it.status, equalTo(status))

    if (error == null) {
        assertThat(it.message, absent())
    } else {
        assertThat(
            ErrorResponse.parse(Body(it.message!!.trim()).xmlDoc()).error,
            equalTo(error)
        )
    }
}
