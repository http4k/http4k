/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.TenantSqlContract
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.jdbc.select

object HatchNotes : TenantTable("hatch_notes") {
    val id = varchar("id", 100)
    val text = varchar("text", 255)
}

object HatchTags : TenantTable("hatch_tags") {
    val note = varchar("note", 100)
    val tag = varchar("tag", 100)
}

abstract class ExposedTenantSqlContract : TenantSqlContract() {
    override fun add(tables: Tables, id: String, text: String) = tables.exposed {
        insert(HatchNotes) {
            it[this.id] = id
            it[this.text] = text
        }
    }

    override fun tag(tables: Tables, note: String, tag: String) = tables.exposed {
        insert(HatchTags) {
            it[this.note] = note
            it[this.tag] = tag
        }
    }

    override fun texts(tables: Tables) = tables.exposed { query { HatchNotes.select(HatchNotes.text) }.map { it[HatchNotes.text] } }

    override fun tagged(tables: Tables, tag: String) = tables.exposed {
        query {
            HatchNotes.join(HatchTags, JoinType.INNER, HatchNotes.id, HatchTags.note)
                .select(HatchNotes.text)
                .where { HatchTags.tag eq tag }
        }.map { it[HatchNotes.text] }
    }

    override fun taggedViaSubquery(tables: Tables, tag: String) = tables.exposed {
        query {
            HatchNotes.select(HatchNotes.text)
                .where { HatchNotes.id inSubQuery HatchTags.select(HatchTags.note).where { HatchTags.tag eq tag } }
        }.map { it[HatchNotes.text] }
    }

    override fun rename(tables: Tables, id: String, text: String) = tables.exposed {
        update(HatchNotes, { HatchNotes.id eq id }) { it[this.text] = text }
    }

    override fun remove(tables: Tables, id: String) = tables.exposed { delete(HatchNotes) { HatchNotes.id eq id } }
}
