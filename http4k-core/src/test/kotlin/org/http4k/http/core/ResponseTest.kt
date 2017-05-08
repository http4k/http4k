package org.http4k.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.http.core.Response.Companion.badRequest
import org.http4k.http.core.Response.Companion.found
import org.http4k.http.core.Response.Companion.movedPermanently
import org.http4k.http.core.Response.Companion.movedTemporarily
import org.http4k.http.core.Response.Companion.notFound
import org.http4k.http.core.Response.Companion.ok
import org.http4k.http.core.Response.Companion.serverError
import org.junit.Test

class ResponseTest {
    @Test
    fun can_create_most_common_responses() {
        assertThat(ok().status, equalTo(Status.OK))
        assertThat(notFound().status, equalTo(Status.NOT_FOUND))
        assertThat(badRequest().status, equalTo(Status.BAD_REQUEST))
        assertThat(serverError().status, equalTo(Status.INTERNAL_SERVER_ERROR))
        assertThat(movedPermanently().status, equalTo(Status.MOVED_PERMANENTLY))
        assertThat(movedTemporarily().status, equalTo(Status.FOUND))
        assertThat(found().status, equalTo(Status.FOUND))
    }
}