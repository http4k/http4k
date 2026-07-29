/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.auto
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiString
import org.http4k.lens.MetaKey
import org.http4k.lens.baggage
import org.http4k.lens.progressToken
import org.http4k.lens.traceParent
import org.http4k.lens.traceState
import org.junit.jupiter.api.Test

class MetaKeyTest {

    private val progressToken = MetaKey.progressToken<String>().toLens()
    private val traceParent = MetaKey.traceParent().toLens()
    private val traceState = MetaKey.traceState().toLens()
    private val baggage = MetaKey.baggage().toLens()

    @Test
    fun `progressToken roundtrip`() {
        val token = "my-token"
        val meta = Meta(progressToken of token)
        assertThat(progressToken(meta), equalTo(token))
    }

    @Test
    fun `optional field returns null when missing`() {
        val meta = Meta()
        assertThat(progressToken(meta), absent())
    }

    @Test
    fun `traceparent roundtrip`() {
        val value = "00-abc-def-01"
        val meta = Meta(traceParent of value)
        assertThat(traceParent(meta), equalTo(value))
    }

    @Test
    fun `multiple fields via with`() {
        val meta = Meta(
            progressToken of "tok",
            traceParent of "00-abc",
            traceState of "vendor=value",
            baggage of "key=val"
        )
        assertThat(progressToken(meta), equalTo("tok" as ProgressToken))
        assertThat(traceParent(meta), equalTo("00-abc"))
        assertThat(traceState(meta), equalTo("vendor=value"))
        assertThat(baggage(meta), equalTo("key=val"))
    }

    @Test
    fun `string key access by name`() {
        val meta = Meta(traceParent of "00-abc")
        assertThat(meta["traceparent"], equalTo(MoshiString("00-abc") as MoshiNode))
    }

    @Test
    fun `serialization roundtrip - with fields`() {
        val meta = Meta(progressToken of "tok", traceParent of "00-abc")
        val json = McpJson.asFormatString(meta)
        val roundTripped = McpJson.asA<Meta>(json)
        assertThat(progressToken(roundTripped), equalTo("tok" as ProgressToken))
        assertThat(traceParent(roundTripped), equalTo("00-abc"))
    }

    @Test
    fun `auto lens roundtrip for custom type`() {
        val customLens = MetaKey.auto(CustomMeta).toLens()
        val custom = CustomMeta("hello", 42)
        val meta = Meta(customLens of custom)
        assertThat(customLens(meta), equalTo(custom))
    }

    @Test
    fun `auto lens serialization roundtrip`() {
        val customLens = MetaKey.auto(CustomMeta).toLens()
        val custom = CustomMeta("hello", 42)
        val meta = Meta(customLens of custom)
        val json = McpJson.asFormatString(meta)
        val roundTripped = McpJson.asA<Meta>(json)
        assertThat(customLens(roundTripped), equalTo(custom))
    }

    @Test
    fun `unknown fields are preserved on roundtrip`() {
        val json = """{"progressToken":"tok","custom_field":"custom_value"}"""
        val meta = McpJson.asA<Meta>(json)
        assertThat(progressToken(meta), equalTo("tok" as ProgressToken))
        assertThat(meta["custom_field"], equalTo(MoshiString("custom_value") as MoshiNode))
        val reJson = McpJson.asFormatString(meta)
        val reMeta = McpJson.asA<Meta>(reJson)
        assertThat(reMeta["custom_field"], equalTo(MoshiString("custom_value") as MoshiNode))
    }
}

data class CustomMeta(val name: String, val count: Int) {
    companion object : MetaField<CustomMeta>("custom")
}
