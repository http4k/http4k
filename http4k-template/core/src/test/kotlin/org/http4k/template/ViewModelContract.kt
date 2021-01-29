package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Test

abstract class ViewModelContract(private val templates: Templates) {

    private val items = listOf(
        Item("item1", "£1", listOf(Feature("pretty"))),
        Item("item2", "£3", listOf(Feature("nasty"))))

    @Test
    fun `renders into Body`() {
        val renderer = templates.CachingClasspath()

        val view = Body.viewModel(renderer, TEXT_HTML).toLens()

        val response = view(OnClasspath(items), Response(OK))

        assertThat(response.bodyString(), equalTo("<ul><li>Name:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>Name:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul>"))
        assertThat(response.status, equalTo(OK))
        assertThat(CONTENT_TYPE(response), equalTo(TEXT_HTML))
    }

    @Test
    fun `renders into WsMessage`() {
        val renderer = templates.CachingClasspath()

        val view = WsMessage.viewModel(renderer).toLens()
        val response = view.create(OnClasspath(items))

        assertThat(response.bodyString(), equalTo("<ul><li>Name:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>Name:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul>"))
    }
}
