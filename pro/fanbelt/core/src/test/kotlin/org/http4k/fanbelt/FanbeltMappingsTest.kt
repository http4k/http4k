/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt

import org.http4k.fanbelt.bus.BackplaneMessage
import org.http4k.fanbelt.bus.ChannelName
import org.http4k.fanbelt.bus.Interest
import org.http4k.fanbelt.bus.NodeId
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.page.RegionId
import org.http4k.fanbelt.presence.ConnectionId
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.state.StateName
import org.http4k.fanbelt.store.Cursor
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.tenancy.RedrawLimit
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal data class EveryValue(
    val region: RegionId = RegionId.of("region"),
    val topic: Topic = Topic.of("topic"),
    val tenant: TenantId = TenantId.of("tenant"),
    val field: FieldName = FieldName.of("field"),
    val state: StateName = StateName.of("state"),
    val channel: ChannelName = ChannelName.of("channel"),
    val node: NodeId = NodeId.of(UUID(0, 1)),
    val message: BackplaneMessage = BackplaneMessage.of("message"),
    val interest: Interest = Interest.of("interest"),
    val connection: ConnectionId = ConnectionId.of(UUID(0, 2)),
    val member: PresenceMember = PresenceMember.of("member"),
    val cursor: Cursor = Cursor.of(3),
    val table: TableName = TableName.of("table"),
    val limit: RedrawLimit = RedrawLimit.of(4),
)

class FanbeltMappingsTest {
    private val json = ConfigurableMoshi(standardConfig().withFanbeltMappings().done())

    @Test
    fun `every value type in the library writes as its bare value and reads back`() {
        val written = json.asFormatString(EveryValue())

        assertEquals(
            """{"region":"region","topic":"topic","tenant":"tenant","field":"field","state":"state","channel":"channel",""" +
                """"node":"00000000-0000-0000-0000-000000000001","message":"message","interest":"interest",""" +
                """"connection":"00000000-0000-0000-0000-000000000002","member":"member","cursor":3,"table":"table","limit":4}""",
            written,
        )
        assertEquals(EveryValue(), json.asA<EveryValue>(written))
    }
}
