package org.http4k.server

import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters

class ApacheServerTest : ServerContract(::ApacheServer, DebuggingFilters.PrintRequestAndResponse().then(ApacheClient()),
    Method.values().filter { it != Method.PURGE }.toTypedArray())
