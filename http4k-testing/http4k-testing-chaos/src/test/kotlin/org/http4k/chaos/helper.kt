package org.http4k.chaos

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.format.Jackson.asJsonObject
import java.time.Duration

internal fun assertBehaviour(json: String, description: String, matcher: Matcher<Response>) {
    val behaviour = json.createBehaviourWith(description)
    val tx = HttpTransaction(Request(GET, ""), Response(OK).body("hello"), Duration.ZERO)
    assertThat(behaviour.then { tx.response }(tx.request), matcher)
}

@JvmName("assertBehaviourRequest")
internal fun assertBehaviour(json: String, description: String, matcher: Matcher<Request>) {
    json.createBehaviourWith(description).then {
        assertThat(it, matcher)
        Response(OK)
    }(Request(GET, "").body("hello"))
}

private fun String.createBehaviourWith(description: String) = asJsonObject().asBehaviour().apply {
    assertThat(toString(), equalTo(description))
}
