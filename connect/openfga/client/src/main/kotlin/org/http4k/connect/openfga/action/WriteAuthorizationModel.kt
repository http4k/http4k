package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.AuthorizationModelWritten
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TypeDefinition
import org.http4k.core.Method.POST
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class WriteAuthorizationModel(val store: StoreId, val typeDefinitions: List<TypeDefinition>) :
    OpenFgaAction<AuthorizationModelWritten>(kClass()) {
    override fun toRequest() = Request(POST, "/stores/$store/authorization-models")
        .json(WriteAuthorizationModelBody(typeDefinitions))
}

@JsonSerializable
data class WriteAuthorizationModelBody(
    val type_definitions: List<TypeDefinition>,
    val schema_version: String = "1.1"
)
