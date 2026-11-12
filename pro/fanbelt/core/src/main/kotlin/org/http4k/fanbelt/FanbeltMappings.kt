/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt

import com.squareup.moshi.Moshi
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
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.value
import org.http4k.lens.BiDiMapping
import java.math.BigDecimal
import java.math.BigInteger

fun AutoMappingConfiguration<Moshi.Builder>.withFanbeltMappings(): AutoMappingConfiguration<Moshi.Builder> = FanbeltMappings(apply {
    value(BackplaneMessage)
    value(ChannelName)
    value(ConnectionId)
    value(Cursor)
    value(FieldName)
    value(Interest)
    value(NodeId)
    value(PresenceMember)
    value(RedrawLimit)
    value(RegionId)
    value(StateName)
    value(TableName)
    value(TenantId)
    value(Topic)
})

private class FanbeltMappings(private val mappings: AutoMappingConfiguration<Moshi.Builder>) : AutoMappingConfiguration<Moshi.Builder> {
    override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) = apply { mappings.boolean(mapping) }
    override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) = apply { mappings.int(mapping) }
    override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) = apply { mappings.long(mapping) }
    override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) = apply { mappings.double(mapping) }
    override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) = apply { mappings.bigInteger(mapping) }
    override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) = apply { mappings.bigDecimal(mapping) }
    override fun <OUT> text(mapping: BiDiMapping<String, OUT>) = apply { mappings.text(mapping) }
    override fun done(): Moshi.Builder = mappings.done().add(FanbeltJsonAdapterFactory)
}
