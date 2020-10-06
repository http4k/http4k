package org.http4k.cloudnative.health

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.Argo
import org.junit.jupiter.api.Test

class JsonReadinessCheckResultRendererTest {

    private val renderer = JsonReadinessCheckResultRenderer(Argo)

    @Test
    fun `calls toString() on successful result`() {
        assertThat(renderer(Completed("bob")), equalTo(Argo {
            pretty(obj(
                "name" to string("bob"),
                "success" to boolean(true)
            ))
        }))
    }

    @Test
    fun `calls toString() on composite result`() {
        val composite = Composite(listOf(Completed("first"), Failed("second", "foobar")))
        assertThat(renderer(composite),
            equalTo(Argo {
                pretty(
                    obj(
                        "name" to string("overall"),
                        "success" to boolean(false),
                        "children" to array(listOf(
                            obj(
                                "name" to string("first"),
                                "success" to boolean(true)
                            ),
                            obj(
                                "name" to string("second"),
                                "success" to boolean(false),
                                "message" to string("foobar")
                            )
                        ))
                    )
                )
            })
        )
    }
}
