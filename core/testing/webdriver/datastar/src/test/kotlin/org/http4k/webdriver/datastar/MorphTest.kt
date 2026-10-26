package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.sameInstance
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

    @Test
    fun `outer mode morphs the target in place`() {
        val d = doc("<html><body><div id='x' class='old'>old</div></body></html>")
        val original = d.selectFirst("#x")

        d.applyPatch(PatchElements("<div id='x' class='new'>new</div>", selector = Selector.of("#x")))

        assertThat(d.selectFirst("#x"), sameInstance(original))
        assertThat(d.selectFirst("#x")?.outerHtml(), equalTo("<div id=\"x\" class=\"new\">new</div>"))
    }

    @Test
    fun `morphing keeps children matched by id and drops the unmatched`() {
        val d = doc("<html><body><ul id='list'><li id='a'>a</li><li id='b'>b</li></ul></body></html>")
        val b = d.selectFirst("#b")

        d.applyPatch(PatchElements("<ul id='list'><li id='b'>b2</li><li id='c'>c</li></ul>"))

        assertThat(d.selectFirst("#b"), sameInstance(b))
        assertThat(d.selectFirst("#list")?.children()?.map { it.text() }, equalTo(listOf("b2", "c")))
    }

    @Test
    fun `morphing leaves data-ignore-morph subtrees untouched`() {
        val d = doc("<html><body><div id='row'><input id='cell' data-ignore-morph value='typed'><span>old</span></div></body></html>")

        d.applyPatch(PatchElements("<div id='row'><input id='cell' data-ignore-morph value='server'><span>new</span></div>"))

        assertThat(d.selectFirst("#cell")?.attr("value"), equalTo("typed"))
        assertThat(d.selectFirst("#row span")?.text(), equalTo("new"))
    }

    @Test
    fun `inner mode morphs the children of the target`() {
        val d = doc("<html><body><div id='x'><span id='s'>old</span></div></body></html>")
        val span = d.selectFirst("#s")

        d.applyPatch(PatchElements("<span id='s'>new</span>", morphMode = MorphMode.inner, selector = Selector.of("#x")))

        assertThat(d.selectFirst("#s"), sameInstance(span))
        assertThat(d.selectFirst("#s")?.text(), equalTo("new"))
    }

    @Test
    fun `replace mode replaces rather than morphs`() {
        val d = doc("<html><body><div id='x' data-ignore-morph>old</div></body></html>")
        val original = d.selectFirst("#x")

        d.applyPatch(PatchElements("<div id='x'>new</div>", morphMode = MorphMode.replace, selector = Selector.of("#x")))

        assertThat(d.selectFirst("#x") === original, equalTo(false))
        assertThat(d.selectFirst("#x")?.text(), equalTo("new"))
    }
}
