package org.http4k.server

import org.http4k.client.ApacheClient

class RatpackTest : ServerContract({ Ratpack(it) }, ApacheClient()) {

    override fun `ok when length already set`() {
    }
}
