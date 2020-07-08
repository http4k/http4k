package org.http4k.servirtium

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.junit.jupiter.api.Test

interface TestContract {
    val uri: Uri

    fun createHandler() = ClientFilters.SetBaseUriFrom(uri).then(JavaHttpClient())

    val control: InteractionControl

    @Test
    @JvmDefault
    fun scenario() {
        val handler = createHandler()

        control.addNote("this is a note")

        val request = Request(POST, "/foobar").header("content-type", "application/x-www-form-urlencoded").body("welcome")

        assertThat(handler(request).bodyString(), equalTo("hello"))

        control.addNote("this is another note")

        assertThat(handler(request).bodyString(), equalTo("hello"))

        control.addNote("this is yet another note")
    }
}
