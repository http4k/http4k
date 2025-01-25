package org.http4k.server

class RatpackTest : ServerContract({ Ratpack(it) }, ClientForServerTesting()) {

    override fun `ok when length already set`() {
    }
}

