package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.model.Assertions
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.GET
import org.http4k.core.Request

@Http4kConnectAction
data class ReadAssertions(val store: StoreId, val model: AuthorizationModelId) : OpenFgaAction<Assertions>(kClass()) {
    override fun toRequest() = Request(GET, "/stores/$store/assertions/$model")
}
