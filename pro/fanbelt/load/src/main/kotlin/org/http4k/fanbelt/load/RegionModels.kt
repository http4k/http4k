/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.load

import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import sl4k.domain.ChannelName
import sl4k.domain.MessageText
import sl4k.domain.ViewerId
import sl4k.domain.Window
import sl4k.feature.Chat
import sl4k.feature.ChatFeature
import sl4k.store.ChatTables
import sl4k.store.Teams
import java.lang.management.ManagementFactory
import java.time.Clock
import java.time.Duration
import java.util.Random
import java.util.UUID

fun regionModels(channels: Int = 20, messages: Int = 1000, windows: List<Int> = listOf(50, 200, 1000)): Report {
    val report = Report(
        "region-models",
        "What a region keeps between changes: the last model per watched key. $channels sl4k channels of $messages messages " +
            "(100-character texts, 10 authors), each channel's history model read once for each window size, held with storage dropped. ${mode()}."
    )
    heldModels(2, 50, 50)
    windows.forEach { show ->
        val before = heapUsed()
        val held = heldModels(channels, messages, show)
        val grown = heapUsed() - before
        report
            .row("Window of $show messages: heap per model", "${grown / held.size / 1024} KB")
            .row("Window of $show messages: heap per 1,000 watched channels", "${grown * 1000 / held.size / (1024 * 1024)} MB")
        check(held.isNotEmpty())
    }
    return report.write()
}

private fun heldModels(channels: Int, messages: Int, show: Int): List<Any> {
    val clock = Clock.systemUTC()
    val random = Random(1)
    val storage = InMemoryDocumentStorage()
    val team = TenantId.of("team-${System.nanoTime()}")
    val names = (1..channels).map { ChannelName.of("channel-$it") }
    val authors = (1..10).map { ViewerId.of(UUID(0, it.toLong())) }
    Teams(storage).create(team, clock, *names.toTypedArray())
    names.forEach { name ->
        storage.write { tenants ->
            val chat = Chat(ChatTables(tenants.tables(team)), clock, random)
            val channel = checkNotNull(chat.channel(name))
            repeat(messages) { chat.post(channel, authors[it % authors.size], MessageText.of("message $it ".padEnd(100, 'x'))) }
        }
    }
    val runtime = AppRuntime(clock = clock, random = random)
    val scope = AppInfra(runtime, storage).scope(team)
    val feature = ChatFeature(runtime)
    return names.map { name -> checkNotNull(scope.read(feature.history, Window(name, show))) }
}

private fun heapUsed(): Long {
    repeat(3) {
        System.gc()
        pause(Duration.ofMillis(300))
    }
    return ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
}
