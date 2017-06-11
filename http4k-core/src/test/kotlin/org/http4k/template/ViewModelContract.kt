package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.junit.Test

abstract class ViewModelContract(private val templates: Templates) {

    private val items = listOf(
        Item("item1", "£1", listOf(Feature("pretty"))),
        Item("item2", "£3", listOf(Feature("nasty"))))

    @Test
    fun `renders from Body`() {
        val renderer = templates.CachingClasspath()

        val view = Body.view(renderer, TEXT_HTML)

        val response = view(OnClasspath(items), Response(OK))

        assertThat(response.bodyString(), equalTo("Name:item1Price:£1Feature:prettyName:item2Price:£3Feature:nasty"))
        assertThat(response.status, equalTo(OK))
        assertThat(CONTENT_TYPE(response), equalTo(TEXT_HTML))
    }
}