package org.http4k.connect.github.api

import org.http4k.connect.github.model.AccountType
import org.http4k.connect.github.model.Email
import org.http4k.connect.github.model.Owner
import org.http4k.core.Status
import org.http4k.core.Uri
import java.time.Instant
import kotlin.math.absoluteValue

data class StoredUser(
    val login: Owner,
    val id: Int = login.hashCode().absoluteValue,
    val name: String,
    val company: String?,
    val emails: List<Email>,
    val type: AccountType,
    val createdAt: Instant,
    val updatedAt: Instant,
    val gistsUrl: Uri = Uri.of("https://api.github.com/users/$login/gists{/gist_id}"),
    val reposUrl: Uri = Uri.of("https://api.github.com/users/$login/repos"),
    val starredUrl: Uri = Uri.of("https://api.github.com/users/$login/starred{/owner}{/repo}"),
    val avatarUrl: Uri = Uri.of("https://avatars.githubusercontent.com/u/$id?v=4"),
    val eventsUrl: Uri = Uri.of("https://api.github.com/users/$login/events{/privacy}"),
    val receivedEventsUrl: Uri = Uri.of("https://api.github.com/users/$login/received_events"),
    val publicRepos: Int = 0,
    val publicGists: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
)

data class GitHubError(
    val message: String,
    val documentation_url: Uri,
    val status: Status
)
