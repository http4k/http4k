package org.http4k.server

import org.http4k.client.ApacheClient
import org.http4k.core.Method

class Apache4ServerTest : ServerContract(
    ::Apache4Server, ApacheClient(),
    Method.values().filter { it != Method.PURGE }.toTypedArray()
)
