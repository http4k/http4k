package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.model.AuthorizationModelFound
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.GET
import org.http4k.core.Request

@Http4kConnectAction
data class ReadAuthorizationModel(val store: StoreId, val id: AuthorizationModelId) :
    OpenFgaAction<AuthorizationModelFound>(kClass()) {
    override fun toRequest() = Request(GET, "/stores/$store/authorization-models/$id")
}
