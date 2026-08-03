/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.Elicitation.Metadata.string.MaxLength
import org.http4k.ai.mcp.model.FooEnum.A
import org.http4k.ai.mcp.model.FooEnum.B
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ElicitationModelTest {
    @Test
    fun `creates schema`(approver: Approver) {
        approver.assertApproved(McpJson.asFormatString(Foo().toSchema()), APPLICATION_JSON)
    }

    @Test
    fun `captures assigned property values`() {
        val foo = Foo().apply {
            s = "asd"
            i = 123
            d = 1.23
            b = true
            e = A
            me = listOf(A, B)
        }

        assertThat(foo.s, equalTo("asd"))
        assertThat(foo.i, equalTo(123))
        assertThat(foo.d, equalTo(1.23))
        assertThat(foo.b, equalTo(true))
        assertThat(foo.e, equalTo(A))
        assertThat(foo.me, equalTo(listOf(A, B)))
    }
}

class Foo : ElicitationModel() {
    var s by string("s", "the s", null, MaxLength(10))
    var os by optionalString("os", "the os")
    var e by enum(
        "e",
        "the e",
        FooEnum.entries.associateWith { it.name.lowercase() },
        A
    )
    var me by enums(
        "e",
        "the e",
        FooEnum.entries.associateWith { it.name.lowercase() },
        listOf(A, B)
    )
    var oe by optionalEnum<FooEnum>("oe", "the oe")
    var l by long("l", "the l")
    var ol by optionalLong("ol", "the ol")
    var i by int("i", "the i")
    var oi by optionalInt("oi", "the oi")
    var d by double("d", "the d")
    var od by optionalDouble("od", "the od")
    var b by boolean("b", "the b")
    var ob by optionalBoolean("ob", "the ob")
}

enum class FooEnum {
    A, B, C
}
