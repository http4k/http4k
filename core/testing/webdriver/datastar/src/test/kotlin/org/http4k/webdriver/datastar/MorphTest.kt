package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.MorphMode
import org.http4k.datastar.Selector
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test

class MorphTest {

    private fun doc(html: String) = Jsoup.parse(html)

    @Test
    fun `outer mode replaces the target with the fragment`() {
        val d = doc("<html><body><div id='x'>old</div></body></html>")

        d.applyPatch(PatchElements("<p id='x'>new</p>", selector = Selector.of("#x")))

        assertThat(d.selectFirst("#x")?.tagName(), equalTo("p"))
        assertThat(d.selectFirst("#x")?.text(), equalTo("new"))
    }

    @Test
    fun `inner mode replaces the children of the target`() {
        val d = doc("<html><body><div id='x'>old</div></body></html>")

        d.applyPatch(PatchElements("<span>new</span>", morphMode = MorphMode.inner, selector = Selector.of("#x")))

        assertThat(d.selectFirst("#x")?.html(), equalTo("<span>new</span>"))
    }

    @Test
    fun `append mode adds children`() {
        val d = doc("<html><body><ul id='list'><li>a</li></ul></body></html>")

        d.applyPatch(PatchElements("<li>b</li>", morphMode = MorphMode.append, selector = Selector.of("#list")))

        assertThat(d.selectFirst("#list")?.children()?.size, equalTo(2))
        assertThat(d.selectFirst("#list")?.children()?.last()?.text(), equalTo("b"))
    }

    @Test
    fun `prepend mode adds children to the front`() {
        val d = doc("<html><body><ul id='list'><li>a</li></ul></body></html>")

        d.applyPatch(PatchElements("<li>z</li>", morphMode = MorphMode.prepend, selector = Selector.of("#list")))

        assertThat(d.selectFirst("#list")?.children()?.first()?.text(), equalTo("z"))
    }

    @Test
    fun `remove mode deletes the target`() {
        val d = doc("<html><body><div id='x'>old</div></body></html>")

        d.applyPatch(PatchElements("<unused/>", morphMode = MorphMode.remove, selector = Selector.of("#x")))

        assertThat(d.selectFirst("#x"), equalTo(null))
    }

    @Test
    fun `no selector matches existing element by id`() {
        val d = doc("<html><body><div id='x'>old</div><div id='y'>keep</div></body></html>")

        d.applyPatch(PatchElements("<div id='x'>new</div>"))

        assertThat(d.selectFirst("#x")?.text(), equalTo("new"))
        assertThat(d.selectFirst("#y")?.text(), equalTo("keep"))
    }

    @Test
    fun `before mode inserts a sibling before the target`() {
        val d = doc("<html><body><div id='anchor'>anchor</div></body></html>")

        d.applyPatch(PatchElements("<div class='inserted'>before</div>", morphMode = MorphMode.before, selector = Selector.of("#anchor")))

        assertThat(d.body().html(), containsSubstring("inserted"))
        assertThat(d.selectFirst(".inserted")?.nextElementSibling()?.id(), equalTo("anchor"))
    }
}
