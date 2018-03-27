package org.http4k.server

import org.http4k.client.ApacheClient
import org.junit.Ignore
import org.junit.Test

class SunHttpTest : ServerContract(::SunHttp, ApacheClient()) {

    @Test
    @Ignore("at the moment, we don't support length in non-body operations")
    override
    fun `length is zero on GET body`() {
    }

}