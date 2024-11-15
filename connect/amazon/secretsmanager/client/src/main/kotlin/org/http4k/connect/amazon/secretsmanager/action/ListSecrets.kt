package org.http4k.connect.amazon.secretsmanager.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.amazon.secretsmanager.SecretsManagerAction
import org.http4k.connect.amazon.secretsmanager.model.Filter
import org.http4k.connect.amazon.secretsmanager.model.Secret
import org.http4k.connect.amazon.secretsmanager.model.SortOrder
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListSecrets(
    val MaxResults: Int? = null,
    val NextToken: String? = null,
    val SortOrder: SortOrder? = null,
    val Filters: List<Filter>? = null
) : SecretsManagerAction<Secrets>(Secrets::class),
    PagedAction<String, Secret, Secrets, ListSecrets> {
    override fun next(token: String) = copy(NextToken = token)
}

@JsonSerializable
data class Secrets(
    val SecretList: List<Secret>,
    val NextToken: String? = null
) : Paged<String, Secret> {
    override fun token() = NextToken
    override val items = SecretList
}

