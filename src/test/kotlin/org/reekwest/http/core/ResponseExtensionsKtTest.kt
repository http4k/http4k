package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK

class ResponseExtensionsKtTest {
    @Test
    fun can_create_response_using_header_entity() {
        assertThat(ok(), equalTo(Response(OK)))
        assertThat(notFound(), equalTo(Response(NOT_FOUND)))
    }
}