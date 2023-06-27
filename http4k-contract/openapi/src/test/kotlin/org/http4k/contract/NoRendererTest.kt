package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.security.NoSecurity
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.LensFailure
import org.junit.jupiter.api.Test

class NoRendererTest {
    @Test
    fun `renders not found`() {
        assertThat(NoRenderer.notFound(), equalTo(Response(NOT_FOUND)))
    }

    @Test
    fun `renders bad request`() {
        assertThat(NoRenderer.badRequest(LensFailure(listOf())), equalTo(Response(BAD_REQUEST)))
    }

    @Test
    fun `renders description`() {
        assertThat(NoRenderer.description(Root, NoSecurity, listOf(), emptySet(), emptyMap()), equalTo(Response(OK)))
    }
}
