package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Method.*
import org.reekwest.http.core.Uri.Companion.uri

class RequestExtensionsTest {

    @Test
    fun can_create_using_method_and_uri_string() {
        assertThat(get("/uri"), equalTo(Request(GET, uri("/uri"))))
        assertThat(post("/uri"), equalTo(Request(POST, uri("/uri"))))
        assertThat(put("/uri"), equalTo(Request(PUT, uri("/uri"))))
    }
}