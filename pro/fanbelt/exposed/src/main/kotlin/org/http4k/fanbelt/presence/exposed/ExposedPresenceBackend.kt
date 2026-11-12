/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence.exposed

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.presence.PresenceBackend
import org.http4k.fanbelt.presence.PresenceConnection
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.format.AutoMarshalling
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.Connection
import java.time.Clock
import java.time.Duration
import kotlin.reflect.KClass

class ExposedPresenceBackend(
    private val db: Database,
    private val clock: Clock,
    private val json: AutoMarshalling = Moshi,
    private val presence: PresenceTable = PresenceTable(),
) : PresenceBackend {

    override fun join(connections: List<PresenceConnection>, ttl: Duration) {
        if (connections.isEmpty()) return
        val expiresAt = clock.millis() + ttl.toMillis()
        val ordered = connections.sortedWith(compareBy({ it.topic.value }, { it.id.toString() }))
        transaction(db) {
            presence.batchUpsert(ordered) { connection ->
                this[presence.key] = connection.topic.value
                this[presence.connection] = connection.id.toString()
                this[presence.who] = connection.member.value
                this[presence.expiresAt] = expiresAt
            }
            val table = identity(presence)
            val key = identity(presence.key)
            val who = identity(presence.who)
            val value = identity(presence.value)
            val own = identity(presence.connection)
            val jdbc = connection.connection
            check(jdbc is Connection) { "Expected a JDBC connection but got $jdbc" }
            jdbc.prepareStatement(
                "UPDATE $table SET $value = (SELECT MAX(s.$value) FROM $table s WHERE s.$key = $table.$key AND s.$who = $table.$who) " +
                    "WHERE $value IS NULL AND $own = ANY(?)",
            ).use {
                it.setArray(1, jdbc.createArrayOf("varchar", ordered.map { connection -> connection.id.toString() }.toTypedArray()))
                it.executeUpdate()
            }
        }
    }

    override fun leave(connection: PresenceConnection) {
        transaction(db) { presence.deleteWhere { (presence.key eq connection.topic.value) and (presence.connection eq connection.id.toString()) } }
    }

    override fun set(topic: Topic, member: PresenceMember, value: Any) {
        transaction(db) { presence.update({ live(topic) and (presence.who eq member.value) }) { it[presence.value] = json.asFormatString(value) } }
    }

    override fun <V : Any> present(topic: Topic, type: KClass<V>) = transaction(db) {
        val carried = presence.value.max()
        presence.select(presence.who, carried).where { live(topic) }.groupBy(presence.who)
            .associate { PresenceMember.of(it[presence.who]) to it[carried]?.let { text -> json.asA(text, type) } }
    }

    override fun sweep() = transaction(db) {
        val now = clock.millis()
        presence.select(presence.key).where { presence.expiresAt lessEq now }.map { Topic.of(it[presence.key]) }.toSet()
            .also { presence.deleteWhere { presence.expiresAt lessEq now } }
    }

    private fun live(topic: Topic) = (presence.key eq topic.value) and (presence.expiresAt greater clock.millis())
}
