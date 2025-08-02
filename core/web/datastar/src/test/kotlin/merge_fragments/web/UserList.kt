package merge_fragments.web

import merge_fragments.User
import merge_fragments.Users
import merge_fragments.fragments.ListUsers
import org.http4k.core.Method.GET
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.sendMergeFragments
import org.http4k.template.DatastarFragmentRenderer

fun UserList(renderer: DatastarFragmentRenderer, users: Users) = "/users" bind sse(
    GET to sse {
        var memo = listOf<User.Saved>()
        Thread.startVirtualThread {
            val latest = users.list()
            if (memo != latest) {
                it.sendMergeFragments(renderer(ListUsers(latest)))
                memo = latest
            }
            Thread.sleep(1000)
        }
        it.sendMergeFragments(renderer())
    }
)
