package org.http4k.template

import AtRootBobRocker
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Header
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Test

class RockerTemplatesTest : TemplatesContract<RockerTemplates>(RockerTemplates()) {
    override fun onClasspathViewModel(items: List<Item>) = OnClasspathRocker().items(items)

    override fun atRootViewModel(items: List<Item>) = AtRootBobRocker().items(items)

    override fun onClasspathNotAtRootViewModel(items: List<Item>) = OnClasspathNotAtRootRocker().items(items)
}

class RockerViewModelContract() {
    private val templates = RockerTemplates()

    private val items = listOf(
        Item("item1", "£1", listOf(Feature("pretty"))),
        Item("item2", "£3", listOf(Feature("nasty"))))

    @Test
    fun `renders into Body`() {
        val renderer = templates.CachingClasspath()

        val view = Body.viewModel(renderer, ContentType.TEXT_HTML).toLens()

        val response = view(OnClasspathRocker().items(items), Response(Status.OK))

        assertThat(response.bodyString(), equalTo("<ul><li>Name:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>Name:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul>"))
        assertThat(response.status, equalTo(Status.OK))
        assertThat(Header.CONTENT_TYPE(response), equalTo(ContentType.TEXT_HTML))
    }

    @Test
    fun `renders into WsMessage`() {
        val renderer = templates.CachingClasspath()

        val view = WsMessage.viewModel(renderer).toLens()
        val response = view.create(OnClasspathRocker().items(items))

        assertThat(response.bodyString(), equalTo("<ul><li>Name:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>Name:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul>"))
    }
}