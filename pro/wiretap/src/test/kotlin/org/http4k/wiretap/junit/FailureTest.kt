package org.http4k.wiretap.junit

import App
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(Intercept::class)
class FailureTest {

    @Test
    @Disabled
    fun `it fails`() {
        App({ _: Request -> Response(OK) })(Request(Method.GET, ""))
        assertThat(false, equalTo(true));
    }
}
