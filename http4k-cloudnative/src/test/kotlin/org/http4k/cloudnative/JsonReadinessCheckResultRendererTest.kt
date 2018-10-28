package org.http4k.cloudnative

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.Argo
import org.junit.jupiter.api.Test

class JsonReadinessCheckResultRendererTest {

    private val renderer = JsonReadinessCheckResultRenderer(Argo)

    @Test
    fun `calls toString() on successful result`() {
        assertThat(renderer(Completed("name")), equalTo(Argo {
            pretty(obj("name" to boolean(true)))
        }))
    }

    @Test
    fun `calls toString() on composite result`() {
        val composite = Composite(listOf(Completed("first"), Failed("second", "foobar")))
        assertThat(renderer(composite),
            equalTo(Argo {
                pretty(
                    obj(
                        "success" to boolean(false),
                        "children" to obj(
                            "first" to boolean(true),
                            "second" to boolean(false)
                        )
                    )
                )
            })
        )

    }
}