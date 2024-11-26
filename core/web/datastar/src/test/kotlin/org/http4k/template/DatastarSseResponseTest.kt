package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.sse.SseResponse
import org.http4k.template.DatastarSseResponse.MergeFragments
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test

class DatastarSseResponseTest {

    @Test
    fun `when match, passes a consumer with the matching request`() {

        val consumer = MergeFragments(object : ViewModel {}).invoke {
            """<foo>bar</foo>
                |<bar>foo</bar>
            """.trimMargin()
        }.consumer

        val received = { req: Request ->
            assertThat(req, equalTo(Request(GET, "/")))
            SseResponse(consumer)
        }.testSseClient(Request(GET, "/")).received()

        assertThat(received.first().toMessage(), equalTo("""event: datastar-merge-fragments
data: fragments <foo>bar</foo><bar>foo</bar>
data: settleDuration 300
data: mergeMode morph
data: useViewTransition false

"""))
    }
}
