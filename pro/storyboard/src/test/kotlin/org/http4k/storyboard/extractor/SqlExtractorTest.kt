/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.extractor

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.storyboard.EventContext
import org.http4k.storyboard.StoryFrame.Level.Detail
import org.http4k.storyboard.frame.CodeFrame
import org.http4k.storyboard.otel.SpanSnapshot
import org.junit.jupiter.api.Test
import java.util.Base64

class SqlExtractorTest {

    @Test
    fun `produces a Code frame for a db span carrying db_statement`() {
        val event = SpanSnapshot.Event(
            name = "SELECT users",
            epochNanos = 0L,
            attributes = mapOf(
                "db.statement" to "SELECT * FROM users WHERE id = ?",
                "db.system" to "postgresql",
                "db.operation" to "SELECT"
            )
        )

        val frame = SqlExtractor(EventContext(emptySpan(), event)) as CodeFrame
        assertThat(frame.title, equalTo("postgresql SELECT"))
        assertThat(frame.level, equalTo(Detail))
        val html = String(Base64.getDecoder().decode(frame.dom))
        assertThat(html, containsSubstring("language-sql"))
        assertThat(html, containsSubstring("SELECT * FROM users WHERE id = ?"))
    }

    @Test
    fun `recognises new SemConv db_query_text and falls back to first-word operation`() {
        val event = SpanSnapshot.Event(
            name = "insert",
            epochNanos = 0L,
            attributes = mapOf(
                "db.query.text" to "INSERT INTO logs(message) VALUES(?)",
                "db.system.name" to "sqlite"
            )
        )

        val frame = SqlExtractor(EventContext(emptySpan(), event)) as CodeFrame
        assertThat(frame.title, equalTo("sqlite INSERT"))
        val html = String(Base64.getDecoder().decode(frame.dom))
        assertThat(html, containsSubstring("INSERT INTO logs(message) VALUES(?)"))
    }

    @Test
    fun `skips events with no db attributes`() {
        val event = SpanSnapshot.Event(
            name = "irrelevant",
            epochNanos = 0L,
            attributes = mapOf("foo" to "bar")
        )

        assertThat(SqlExtractor(EventContext(emptySpan(), event)), equalTo(null))
    }

    @Test
    fun `escapes SQL angle brackets in the embedded HTML`() {
        val event = SpanSnapshot.Event(
            name = "select",
            epochNanos = 0L,
            attributes = mapOf(
                "db.statement" to "SELECT * FROM tags WHERE name <> '<x>'",
                "db.system" to "mysql"
            )
        )

        val frame = SqlExtractor(EventContext(emptySpan(), event)) as CodeFrame
        val html = String(Base64.getDecoder().decode(frame.dom))
        assertThat(html, containsSubstring("&lt;&gt;"))
        assertThat(html, containsSubstring("&lt;x&gt;"))
    }
}

private fun emptySpan() = SpanSnapshot(
    name = "",
    traceId = "",
    spanId = "",
    parentSpanId = null,
    kind = "CLIENT",
    startEpochNanos = 0L,
    endEpochNanos = 0L,
    attributes = emptyMap(),
    events = emptyList(),
    statusCode = "UNSET"
)
