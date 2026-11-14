package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.DELETE
import org.http4k.core.Request

@Http4kConnectAction
data class DeleteStore(val store: StoreId) : OpenFgaAction<Unit>(Unit::class) {
    override fun toRequest() = Request(DELETE, "/stores/$store")
}
