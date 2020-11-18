package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.jupiter.api.Test

internal class AndRouterTest {

    private val r = Request(Method.GET, "")

    @Test
    fun `unique entry`() {
        val router = AndRouter.from(listOf(MatchRouter))
        assertThat(router.match(r), equalTo(RouterMatch.MatchedWithoutHandler(MatchRouter.getDescription())))
    }

    @Test
    fun `two matches`() {
        val router = AndRouter.from(listOf(MatchRouter, MatchRouter))
        assertThat(router.match(r), equalTo(RouterMatch.MatchedWithoutHandler(router.getDescription(), listOf(MatchRouter.match(r), MatchRouter.match(r)))))

        println(router.match(r))
    }
}

object MatchRouter : Router {
    override fun match(request: Request): RouterMatch = RouterMatch.MatchedWithoutHandler(getDescription())

    override fun getDescription(): RouterDescription = RouterDescription("always match")
}
