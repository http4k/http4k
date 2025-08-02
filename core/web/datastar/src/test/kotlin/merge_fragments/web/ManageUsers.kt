package merge_fragments.web

import merge_fragments.User
import merge_fragments.Users
import merge_fragments.fragments.AddUser
import merge_fragments.fragments.EditUser
import merge_fragments.fragments.ListUsers
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PATCH
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Moshi.datastarModel
import org.http4k.lens.Path
import org.http4k.lens.datastarFragments
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarFragmentRenderer

val userId = Path.int().of("id")

fun manageUsers(renderer: DatastarFragmentRenderer, users: Users) = "/users" bind routes(
    "/{id}" bind routes(
        "/edit" bind GET to {
            when (val user = users.get(userId(it))) {
                null -> Response(NOT_FOUND)
                else -> Response(OK).datastarFragments(renderer(EditUser(user)))
            }
        },
        "/update" bind PATCH to {
            when (users.update(it.datastarModel<User.New>().withId(userId(it)))) {
                null -> Response(NOT_FOUND)
                else -> Response(OK).datastarFragments(renderer(ListUsers(users.list())))
            }
        },
        "/delete" bind DELETE to {
            when (users.delete(userId(it))) {
                null -> Response(NOT_FOUND)
                else -> Response(OK).datastarFragments(renderer(ListUsers(users.list())))
            }
        }
    ),
    "/add" bind GET to {
        Response(OK).datastarFragments(renderer(AddUser))
    },
    "/create" bind POST to {
        users.add(it.datastarModel<User.New>())
        Response(ACCEPTED).datastarFragments(renderer(ListUsers(users.list())))
    }
)
