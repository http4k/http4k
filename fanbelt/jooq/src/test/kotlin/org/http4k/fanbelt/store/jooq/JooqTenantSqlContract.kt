package org.http4k.fanbelt.store.jooq

import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.TenantSqlContract
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.table

private val notes = table(name("hatch_notes"))
private val tags = table(name("hatch_tags"))
private val noteId = field(name("hatch_notes", "id"), String::class.java)
private val noteText = field(name("hatch_notes", "text"), String::class.java)
private val tagNote = field(name("hatch_tags", "note"), String::class.java)
private val tagName = field(name("hatch_tags", "tag"), String::class.java)

abstract class JooqTenantSqlContract : TenantSqlContract() {
    override fun add(tables: Tables, id: String, text: String) {
        tables.jooq { it.insertInto(notes, field(name("id")), field(name("text"))).values(id, text).execute() }
    }

    override fun tag(tables: Tables, note: String, tag: String) {
        tables.jooq { it.insertInto(tags, field(name("note")), field(name("tag"))).values(note, tag).execute() }
    }

    override fun texts(tables: Tables) = tables.jooq { it.select(noteText).from(notes).fetch(noteText) }

    override fun tagged(tables: Tables, tag: String) = tables.jooq {
        it.select(noteText).from(notes.join(tags).on(tagNote.eq(noteId))).where(tagName.eq(tag)).fetch(noteText)
    }

    override fun taggedViaSubquery(tables: Tables, tag: String) = tables.jooq {
        it.select(noteText).from(notes).where(noteId.`in`(select(tagNote).from(tags).where(tagName.eq(tag)))).fetch(noteText)
    }

    override fun rename(tables: Tables, id: String, text: String) =
        tables.jooq { it.update(notes).set(field(name("text"), String::class.java), text).where(noteId.eq(id)).execute() }

    override fun remove(tables: Tables, id: String) = tables.jooq { it.deleteFrom(notes).where(noteId.eq(id)).execute() }

    override fun renameTagged(tables: Tables, tag: String, text: String) = tables.jooq {
        it.update(notes).set(field(name("text"), String::class.java), text).where(noteId.`in`(select(tagNote).from(tags).where(tagName.eq(tag)))).execute()
    }

    override fun withTags(tables: Tables) = tables.jooq {
        it.select(noteText, tagName).from(notes.leftJoin(tags).on(tagNote.eq(noteId))).fetch { row -> row.value1() to row.value2() }
    }

    override fun textsViaAlias(tables: Tables) = tables.jooq {
        val aliased = notes.`as`("n")
        val text = field(name("n", "text"), String::class.java)
        it.select(text).from(aliased).fetch(text)
    }
}
