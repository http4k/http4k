package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.junit.jupiter.api.Test

class FilterExtensionsTest {
    @Test
    fun `set base aws service url`() {
        val app = ClientFilters.SetAwsServiceUrl("myservice", "narnia")
            .then { Response(Status.OK).body(it.uri.toString()) }
        assertThat(app(Request(Method.GET, "/bob")), hasBody("https://myservice.narnia.amazonaws.com/bob"))
    }
}
