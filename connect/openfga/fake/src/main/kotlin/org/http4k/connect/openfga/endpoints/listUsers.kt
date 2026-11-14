package org.http4k.connect.openfga.endpoints

import org.http4k.connect.openfga.FgaStore
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.action.ListUsersBody
import org.http4k.connect.openfga.action.ListedUser
import org.http4k.connect.openfga.action.ListedUsers
import org.http4k.connect.openfga.action.ListedUserset
import org.http4k.connect.openfga.action.ListedWildcard
import org.http4k.connect.openfga.action.TypedObject
import org.http4k.connect.openfga.model.FgaObject
import org.http4k.connect.openfga.model.Relation
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TypeName
import org.http4k.connect.openfga.model.User
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind

fun listUsers(stores: MutableMap<StoreId, FgaStore>) =
    "/stores/{store}/list-users" bind POST to { req ->
        val body = req.json<ListUsersBody>()
        stores.withEvaluation(req, body.authorization_model_id) { evaluation ->
            val obj = FgaObject.of("${body.obj.type}:${body.obj.id}")
            val users = evaluation.with(body.contextual_tuples).listUsers(obj, body.relation, body.user_filters.first())
            Response(OK).json(ListedUsers(users.map { it.listed() }))
        }
    }

private fun User.listed(): ListedUser {
    val type = TypeName.of(value.substringBefore(':'))
    val id = value.substringAfter(':').substringBefore('#')
    return when {
        id == "*" -> ListedUser(wildcard = ListedWildcard(type))
        '#' in value -> ListedUser(userset = ListedUserset(type, id, Relation.of(value.substringAfter('#'))))
        else -> ListedUser(obj = TypedObject(type, id))
    }
}
