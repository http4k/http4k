package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class DustTemplatesTest : TemplatesContract<DustTemplates>(DustTemplates())

class DustViewModelTest : ViewModelContract(DustTemplates())

class DustWithHelpersTest {
    val dust = DustTemplates(dustPluginScripts = listOf(DustTemplatesTest::class.java.getResource("dust-helpers.js")))
        .CachingClasspath("org.http4k.template")

    data class Score(val player: String, val score: Int)
    data class NeedsHelpers(val items: List<String>) : ViewModel {
        override fun template() = "NeedsHelpers"
    }

    @Test
    fun `loads helpers`() {
        val expanded = dust(NeedsHelpers(listOf("alice", "bob", "carol")))
        assertThat(expanded, equalTo("alice, bob, carol"))
    }
}