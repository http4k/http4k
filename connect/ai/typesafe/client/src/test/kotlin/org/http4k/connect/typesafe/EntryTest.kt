package org.http4k.connect.typesafe

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.MoshiArray
import org.http4k.format.MoshiDecimal
import org.http4k.format.MoshiInteger
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNull
import org.http4k.format.MoshiObject
import org.http4k.format.MoshiString
import org.junit.jupiter.api.Test

data class Widget(
    val name: String,
    val count: Int,
    val tags: List<String>,
    val scores: Map<String, Double>
)

fun aWidget(
    name: String = "sprocket",
    count: Int = 42,
    tags: List<String> = listOf("small", "brass"),
    scores: Map<String, Double> = mapOf("quality" to 0.5)
) = Widget(name, count, tags, scores)

class EntryTest {

    @Test
    fun `data class marshals as its json object`() {
        assertThat(
            aWidget().asEntry(),
            equalTo(
                MoshiObject(
                    "name" to MoshiString("sprocket"),
                    "count" to MoshiInteger(42),
                    "tags" to MoshiArray(MoshiString("small"), MoshiString("brass")),
                    "scores" to MoshiObject("quality" to MoshiDecimal(0.5))
                ) as MoshiNode
            )
        )
    }

    @Test
    fun `data class round trips through a node`() {
        assertThat(aWidget().asEntry().asA<Widget>(), equalTo(aWidget()))
    }

    @Test
    fun `primitives round trip`() {
        assertThat("hello".asEntry().asA<String>(), equalTo("hello"))
        assertThat(42.asEntry().asA<Int>(), equalTo(42))
        assertThat(true.asEntry().asA<Boolean>(), equalTo(true))
        assertThat(1.5.asEntry().asA<Double>(), equalTo(1.5))
    }

    @Test
    fun `value types round trip`() {
        assertThat(Probability.of(0.5).asEntry().asA<Probability>(), equalTo(Probability.of(0.5)))
    }

    @Test
    fun `null becomes the null node`() {
        assertThat(null.asEntry(), equalTo(MoshiNull as MoshiNode))
    }

    @Test
    fun `a node passes through with its exact shape`() {
        assertThat(MoshiDecimal(5.0).asEntry(), equalTo(MoshiDecimal(5.0) as MoshiNode))
    }

    @Test
    fun `nodes nested inside collections pass through with their exact shape`() {
        assertThat(
            listOf(MoshiDecimal(5.0), aWidget()).asEntry(),
            equalTo(MoshiArray(MoshiDecimal(5.0), aWidget().asEntry()) as MoshiNode)
        )
        assertThat(
            mapOf("pre" to MoshiDecimal(5.0)).asEntry(),
            equalTo(MoshiObject("pre" to MoshiDecimal(5.0)) as MoshiNode)
        )
    }

    @Test
    fun `lens converts both directions`() {
        val widget = Entry<Widget>()

        assertThat(widget(aWidget()), equalTo(aWidget().asEntry()))
        assertThat(widget(widget(aWidget())), equalTo(aWidget()))
    }

    @Test
    fun `lens applies across lists and maps of nodes`() {
        val widget = Entry<Widget>()

        assertThat(widget(listOf(aWidget().asEntry())), equalTo(listOf(aWidget())))
        assertThat(widget(mapOf("first" to aWidget().asEntry())), equalTo(mapOf("first" to aWidget())))
    }
}
