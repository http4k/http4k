package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.entity.StringEntity

class ResponseExtensionsKtTest {
    @Test
    fun can_create_response_using_header_entity() {
        assertThat(ok(), equalTo(Response(OK)))
        assertThat(notFound(), equalTo(Response(NOT_FOUND)))
    }

    @Test
    fun can_modify_entity(){
        val testEntity = StringEntity.toEntity("abc")
        assertThat(ok().entity(testEntity).entity, equalTo(testEntity))
    }
}