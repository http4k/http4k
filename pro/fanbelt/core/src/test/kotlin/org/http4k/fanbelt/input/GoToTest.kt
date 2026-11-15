/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.junit.jupiter.api.Test

class GoToTest {
    private val request = Request(POST, "/lots")

    @Test
    fun `accepted appends a script to the page that moves the browser to where the result says`() {
        val body = GoTo<String> { lot -> Uri.of("lots/$lot") }.accepted(request, "7", emptyMap()).bodyString()

        assertThat(body, containsSubstring("""<script>window.location.assign("lots/7")</script>"""))
        assertThat(body, containsSubstring("selector body"))
    }

    @Test
    fun `a location can't break out of the script`() {
        val body = GoTo<Unit> { Uri.of("x\"); alert(1); (\"</script><script>alert(2)") }.accepted(request, Unit, emptyMap()).bodyString()

        assertThat(body, containsSubstring("""\"); alert(1); (\"<\/script><script>alert(2)"""))
        assertThat(body, Regex("</script>").findAll(body).count(), equalTo(1))
    }

    @Test
    fun `a refusal is answered by the feedback it wraps`() {
        val refused = GoTo<Unit> { Uri.of("anywhere") }.rejected(Rejected(FieldName.of("title"), "A title is needed"))

        assertThat(refused.bodyString(), containsSubstring("A title is needed"))
    }
}
