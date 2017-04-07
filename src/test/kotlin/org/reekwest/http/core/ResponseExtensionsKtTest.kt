package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.body.body
import org.reekwest.http.core.body.toBody

class ResponseExtensionsKtTest {
    @Test
    fun can_create_response_using_header_and_body() {
        assertThat(ok(), equalTo(Response(OK)))
        assertThat(notFound(), equalTo(Response(NOT_FOUND)))
    }

    @Test
    fun can_modify_body(){
        val testBody = "abc".toBody()
        assertThat(ok().body(testBody).body, equalTo(testBody))
    }
}