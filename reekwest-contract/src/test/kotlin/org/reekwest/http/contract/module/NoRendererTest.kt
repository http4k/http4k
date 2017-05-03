package org.reekwest.http.contract.module

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status

class NoRendererTest {
    @Test
    fun `renders not found`() {
        assertThat(NoRenderer.notFound(), equalTo(Response(Status.NOT_FOUND)))
    }

    @Test
    fun `renders bad request`() {
        assertThat(NoRenderer.badRequest(listOf()), equalTo(Response(Status.BAD_REQUEST)))
    }

    @Test
    fun `renders description`() {
        assertThat(NoRenderer.description(Root, listOf()), equalTo(Response(Status.OK)))
    }

}