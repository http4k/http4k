package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.model.StoreName
import org.http4k.connect.openfga.model.Stores
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Query
import org.http4k.lens.value

@Http4kConnectAction
data class ListStores(val name: StoreName? = null) : OpenFgaAction<Stores>(kClass()) {
    override fun toRequest() = Request(GET, "/stores").with(nameQuery of name)
}

private val nameQuery = Query.value(StoreName).optional("name")
