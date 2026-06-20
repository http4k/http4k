package org.http4k.connect.github.api

import dev.forkhandles.result4k.asResultOr
import dev.forkhandles.result4k.map
import org.http4k.connect.github.model.GitHubUser
import org.http4k.connect.github.model.Owner
import org.http4k.connect.storage.get
import org.http4k.core.Method.GET
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.path

internal fun FakeGitHubJson.getAuthedUser() = "user" bind GET to route { owner ->
    users[owner]?.toGitHubUser().asResultOr { notFound}
}

internal fun FakeGitHubJson.getUser() = "users/{owner}" bind GET to route {
    val owner = Owner.parse(path("owner")!!)
    users[owner]?.toGitHubUser().asResultOr { notFound }
}

fun FakeGitHubJson.getAuthedUserEmails() = "user/emails" bind GET to route { owner ->
    users[owner].asResultOr { notFound }.map { it.emails }
}

fun FakeGitHubJson.getAuthedUserPublicEmails() = "user/public_emails" bind GET to route { owner ->
    users[owner].asResultOr { notFound }.map { user -> user.emails.filter { it.visibility == "public" } }
}

private val notFound = GitHubError(
    message = "Not Found",
    documentation_url = Uri.of("https://docs.github.com/rest"),
    status = Status.NOT_FOUND
)

private fun StoredUser.toGitHubUser() = GitHubUser(
    login = login,
    id = id,
    name = name,
    company = company,
    email = emails.firstOrNull { it.primary }?.email,
    type = type,
    created_at = createdAt,
    updated_at = updatedAt,
    repos_url = reposUrl,
    starred_url = starredUrl,
    avatar_url = avatarUrl,
    events_url = eventsUrl,
    received_events_url = receivedEventsUrl,
    gists_url = gistsUrl,
    public_repos = publicRepos,
    public_gists = publicGists,
    followers = followers,
    following = following,
    blog = "",
    plan = null,
    url = null,
    bio = null,
    node_id = "",
    html_url = null,
    location = null,
    hireable = null,
    gravatar_id = "",
    site_admin = false,
    disk_usage = null,
    followers_url = null,
    following_url = null,
    private_gists = null,
    collaborators = null,
    subscriptions_url = null,
    organizations_url = null,
    notification_email = null,
    total_private_repos = null,
    owned_private_repos = null,
    two_factor_authentication = null,
    twitter_username = null
)
