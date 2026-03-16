package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.OK
import org.http4k.websocket.WsStatus.Companion.ABNORMAL_CLOSE
import org.junit.jupiter.api.Test

class ProtocolStatusAdapterTest {

    @Test
    fun `can serialise protocol status`() {
        assertThat(Moshi.asFormatString(OK), equalTo("200"))
        assertThat(Moshi.asFormatString(ABNORMAL_CLOSE), equalTo("1006"))
    }

}
