package org.http4k.connect.github.api.action

import org.http4k.connect.github.model.Owner
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class GitHubUser(
    val login: Owner,
    val id: Int,
    val node_id: String,
    val avatar_url: Uri,
    val gravatar_id: String,
    val url: Uri,
    val html_url: Uri,
    val followers_url: Uri,
    val following_url: Uri,
    val gists_url: Uri,
    val starred_url: Uri,
    val subscriptions_url: Uri,
    val organizations_url: Uri,
    val repos_url: Uri,
    val events_url: Uri,
    val received_events_url: Uri,
    val type: AccountType,
    val site_admin: Boolean,
    val name: String,
    val company: String,
    val blog: String,
    val location: String,
    val email: String,
    val hireable: Boolean,
    val bio: String,
    val twitter_username: String,
    val public_repos: Int,
    val public_gists: Int,
    val followers: Int,
    val following: Int,
    val created_at: String,
    val updated_at: String,
    val private_gists: Int,
    val total_private_repos: Int,
    val owned_private_repos: Int,
    val disk_usage: Int,
    val collaborators: Int,
    val two_factor_authentication: Boolean,
    val plan: Plan
)
