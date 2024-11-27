package org.http4k.server

import org.http4k.core.Method

class Apache4ServerTest : ServerContract(::Apache4Server, ClientForServerTesting(),
    Method.entries.filter { it != Method.PURGE }.toTypedArray())
