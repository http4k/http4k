package org.http4k.connect.typesafe

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.typesafe.action.SystemOne
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.format.MoshiString
import org.junit.jupiter.api.Test

class TypedStateTest {

    private val isUrgent = Question.Noul("is_urgent", "The message conveys urgency")

    @Test
    fun `data class state marshals identically to the equivalent map state`() {
        val typed = SystemOne(aWidget(), isUrgent).toRequest().bodyString()
        val untyped = SystemOne(
            mapOf(
                "name" to "sprocket",
                "count" to 42,
                "tags" to listOf("small", "brass"),
                "scores" to mapOf("quality" to 0.5)
            ),
            isUrgent
        ).toRequest().bodyString()

        assertThat(typed, equalTo(untyped))
    }

    @Test
    fun `a pre-built node state passes through unchanged`() {
        val node = MoshiObject("already" to MoshiString("wrapped"))

        assertThat(SystemOne(node, isUrgent).state, equalTo(node as MoshiNode))
    }

    @Test
    fun `lens-built state matches the plain value`() {
        val widget = Entry<Widget>()

        assertThat(SystemOne(widget(aWidget()), isUrgent).state, equalTo(SystemOne(aWidget(), isUrgent).state))
    }

    @Test
    fun `string state keeps its wire shape`() {
        assertThat(SystemOne("plain", isUrgent).state, equalTo(MoshiString("plain") as MoshiNode))
    }
}
