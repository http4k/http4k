package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import kotlinx.html.TagConsumer
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.li
import kotlinx.html.title
import kotlinx.html.ul
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.http4k.testing.HtmlApprovalTest
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(HtmlApprovalTest::class)
class KotlinxHtmlRendererTest {

    @Test
    fun `renders a full page`(approver: Approver) {
        approver.assertApproved(KotlinxHtmlRenderer.renderToResponse(Page("Todos")))
    }

    @Test
    fun `renders a fragment into a Body`(approver: Approver) {
        val view = Body.viewModel(KotlinxHtmlRenderer, TEXT_HTML).toLens()

        approver.assertApproved(view(todoList(), Response(OK)))
    }

    @Test
    fun `escapes text content`(approver: Approver) {
        approver.assertApproved(
            KotlinxHtmlRenderer.renderToResponse(TodoList(listOf("<script>alert('pwned')</script>")))
        )
    }

    @Test
    fun `renders into a WsMessage`() {
        val view = WsMessage.viewModel(KotlinxHtmlRenderer).toLens()

        assertThat(view.create(todoList()).bodyString(), equalTo(KotlinxHtmlRenderer(todoList())))
    }

    @Test
    fun `throws when the view model is not renderable`() {
        assertThat({ KotlinxHtmlRenderer(object : ViewModel {}) }, throws<ViewNotFound>())
    }

    @Test
    fun `falls back to another renderer when the view is not renderable`() {
        val fallback: TemplateRenderer = { "fallback" }

        assertThat(KotlinxHtmlRenderer.then(fallback)(object : ViewModel {}), equalTo("fallback"))
    }

    private fun todoList() = TodoList(listOf("item1", "item2"))
}

private data class TodoList(val todos: List<String>) : HtmlViewModel {
    override fun TagConsumer<*>.render() {
        ul { todos.forEach { li { +it } } }
    }
}

private data class Page(val name: String) : HtmlViewModel {
    override fun TagConsumer<*>.render() {
        html {
            head { title { +name } }
            body { h1 { +name } }
        }
    }
}
