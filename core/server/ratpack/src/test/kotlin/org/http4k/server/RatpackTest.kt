package org.http4k.server

import kotlinx.coroutines.runBlocking

class RatpackTest : ServerContract(::Ratpack, ClientForServerTesting()) {

    override fun `ok when length already set`() = runBlocking {
    }
}

