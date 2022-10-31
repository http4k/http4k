package org.http4k.format

import argo.jdom.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.Argo.obj
import org.http4k.format.Argo.string
import org.junit.jupiter.api.Test

class ArgoTest : JsonContract<JsonNode>(Argo) {
    override val prettyString = """{
	"hello": "world"
}"""

    @Test
    fun `removes duplicates`() {
        assertThat(Argo.compact(obj("goo" to string("goo"), "goo" to string("goo"))), equalTo("""{"goo":"goo"}"""))
    }
}
