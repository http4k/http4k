@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.openfga.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.ConsistencyPreference
import org.http4k.connect.openfga.model.FgaObject
import org.http4k.connect.openfga.model.Relation
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TupleKey
import org.http4k.connect.openfga.model.TupleKeys
import org.http4k.connect.openfga.model.TypeName
import org.http4k.connect.openfga.model.User
import org.http4k.connect.openfga.model.UserTypeFilter
import org.http4k.connect.openfga.model.Users
import org.http4k.connect.openfga.model.asTupleKeys
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class ListUsers(
    val store: StoreId,
    val obj: FgaObject,
    val relation: Relation,
    val userFilter: UserTypeFilter,
    val contextualTuples: List<TupleKey> = emptyList(),
    val authorizationModel: AuthorizationModelId? = null,
    val consistency: ConsistencyPreference? = null
) : OpenFgaAction<Users>(kClass()) {
    override fun toRequest() = Request(POST, "/stores/$store/list-users").json(
        ListUsersBody(
            TypedObject(TypeName.of(obj.value.substringBefore(':')), obj.value.substringAfter(':')),
            relation,
            listOf(userFilter),
            contextualTuples.asTupleKeys(),
            authorizationModel,
            consistency
        )
    )

    override fun toResult(response: Response): Result<Users, RemoteFailure> = when {
        response.status.successful -> Success(Users(response.json<ListedUsers>().users.map { it.toUser() }))
        else -> Failure(asRemoteFailure(response))
    }
}

@JsonSerializable
data class ListUsersBody(
    @JsonProperty("object")
    val obj: TypedObject,
    val relation: Relation,
    val user_filters: List<UserTypeFilter>,
    val contextual_tuples: TupleKeys? = null,
    val authorization_model_id: AuthorizationModelId? = null,
    val consistency: ConsistencyPreference? = null
)

@JsonSerializable
data class TypedObject(val type: TypeName, val id: String)

@JsonSerializable
data class ListedUsers(val users: List<ListedUser>)

@JsonSerializable
data class ListedUser(
    @JsonProperty("object")
    val obj: TypedObject? = null,
    val userset: ListedUserset? = null,
    val wildcard: ListedWildcard? = null
) {
    fun toUser() = User.of(
        when {
            obj != null -> "${obj.type}:${obj.id}"
            userset != null -> "${userset.type}:${userset.id}#${userset.relation}"
            wildcard != null -> "${wildcard.type}:*"
            else -> error("listed user has no object, userset or wildcard")
        }
    )
}

@JsonSerializable
data class ListedUserset(val type: TypeName, val id: String, val relation: Relation)

@JsonSerializable
data class ListedWildcard(val type: TypeName)
