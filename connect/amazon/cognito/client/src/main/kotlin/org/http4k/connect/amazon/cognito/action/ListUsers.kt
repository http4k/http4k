package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AttributeType
import org.http4k.connect.amazon.cognito.model.MFAOptions
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.amazon.cognito.model.UserStatus
import org.http4k.connect.amazon.core.model.Username
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListUsers(
    val UserPoolId: UserPoolId,
    val AttributesToGet: List<String>? = null, // all attributes
    val Limit: Int = 60,
    val PaginationToken: String? = null
) : CognitoAction<PageOfListedUsers>(PageOfListedUsers::class)

@JsonSerializable
data class PageOfListedUsers(
    val PaginationToken: String?,
    val Users: List<ListedUser>
)

@JsonSerializable
data class ListedUser(
    val Username: Username,
    val Enabled: Boolean,
    val UserCreateDate: Timestamp,
    val UserLastModifiedDate: Timestamp,
    val UserStatus: UserStatus,
    val Attributes: List<AttributeType> = emptyList(),
    val MFAOptions: List<MFAOptions>? = null,
)
