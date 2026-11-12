/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GoToTest {
    private val request = Request(POST, "/lots")

    @Test
    fun `accepted patches a script into the placeholder that moves the browser to where the result says`() {
        val body = GoTo<String> { lot -> Uri.of("lots/$lot") }.accepted(request, "7", emptyMap()).bodyString()

        assertTrue(body.contains("""<script>window.location.assign("lots/7")</script>"""), body)
        assertTrue(body.contains("selector #goto"), body)
    }

    @Test
    fun `a location can't break out of the script`() {
        val body = GoTo<Unit> { Uri.of("x\"); alert(1); (\"</script><script>alert(2)") }.accepted(request, Unit, emptyMap()).bodyString()

        assertTrue(body.contains("""\"); alert(1); (\"<\/script><script>alert(2)"""), body)
        assertEquals(1, Regex("</script>").findAll(body).count(), body)
    }

    @Test
    fun `a script going somewhere can stand in for an element`() {
        assertEquals("""<script id="gate">window.location.assign("c/general")</script>""", goTo(Uri.of("c/general"), id = "gate"))
    }

    @Test
    fun `a refusal is answered by the feedback it wraps`() {
        val refused = GoTo<Unit> { Uri.of("anywhere") }.rejected(Rejected(FieldName.of("title"), "A title is needed"))

        assertTrue(refused.bodyString().contains("A title is needed"))
    }
}
