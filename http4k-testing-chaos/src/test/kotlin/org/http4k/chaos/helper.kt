package org.http4k.chaos

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo

import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.format.Jackson.asJsonObject
import java.time.Duration

internal fun assertBehaviour(json: String, description: String, matcher: Matcher<Response>) {
    val tx = HttpTransaction(Request(GET, ""), Response(Status.OK).body("hello"), Duration.ZERO)
    val behaviour: Behaviour = json.asJsonObject().asBehaviour()
    assertThat(behaviour.toString(), equalTo(description))
    assertThat(behaviour.then { tx.response }(tx.request), matcher)
}

