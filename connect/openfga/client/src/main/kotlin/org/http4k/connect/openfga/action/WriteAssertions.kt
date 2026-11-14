package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.Assertion
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class WriteAssertions(val store: StoreId, val model: AuthorizationModelId, val assertions: List<Assertion>) :
    OpenFgaAction<Unit>(Unit::class) {
    override fun toRequest() = Request(PUT, "/stores/$store/assertions/$model").json(WriteAssertionsBody(assertions))
}

@JsonSerializable
data class WriteAssertionsBody(val assertions: List<Assertion>)
