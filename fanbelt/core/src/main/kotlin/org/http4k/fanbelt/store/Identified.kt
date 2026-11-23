package org.http4k.fanbelt.store

import dev.forkhandles.values.Value

/**
 * A stored document with an id.
 */
interface Identified<ID : Value<*>> {
    val id: ID
}

/**
 * This row, refusing it if a change gave it another id: a row's id is its key, so it can't change.
 */
fun <ID : Value<*>, T : Identified<ID>> T.sameId(id: ID): T =
    apply { require(this.id == id) { "An update can't change a row's id ($id to ${this.id}): delete it and insert another" } }
