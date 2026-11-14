package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.Store
import org.http4k.connect.openfga.model.StoreName
import org.http4k.core.Method.POST
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateStore(val name: StoreName) : OpenFgaAction<Store>(kClass()) {
    override fun toRequest() = Request(POST, "/stores").json(this)
}
