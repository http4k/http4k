package merge_fragments.fragments

import merge_fragments.User
import org.http4k.template.ViewModel

data class ListUsers(val users: List<User.Saved>) : ViewModel
