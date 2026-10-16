package org.http4k.connect.plugin

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ProguardRulesGenerationTest {

    @Test
    fun `keeps defaults constructors of top level kotshi model`() {
        assertThat(rulesFor("org.http4k.connect.plugin.TestBean"), equalTo(expectedRules("org.http4k.connect.plugin.TestBean")))
    }

    @Test
    fun `keeps defaults constructors of nested kotshi model using binary name`() {
        assertThat(
            rulesFor("org.http4k.connect.plugin.TestOuter\$TestNestedBean"),
            equalTo(expectedRules("org.http4k.connect.plugin.TestOuter\$TestNestedBean"))
        )
    }

    private fun rulesFor(binaryName: String) =
        javaClass.getResource("/META-INF/proguard/http4k-kotshi-$binaryName.pro")?.readText()

    private fun expectedRules(binaryName: String) = """
        -keepnames class kotlin.jvm.internal.DefaultConstructorMarker
        -keepclassmembers class $binaryName {
            synthetic <init>(...);
        }

    """.trimIndent()
}
