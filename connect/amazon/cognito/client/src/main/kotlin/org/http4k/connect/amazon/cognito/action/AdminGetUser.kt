package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AttributeType
import org.http4k.connect.amazon.cognito.model.MFAOptions
import org.http4k.connect.amazon.cognito.model.UserMFASetting
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.amazon.cognito.model.UserStatus
import org.http4k.connect.amazon.core.model.Username
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class AdminGetUser(
    val Username: Username,
    val UserPoolId: UserPoolId
) : CognitoAction<ExistingUser>(ExistingUser::class)

@JsonSerializable
data class ExistingUser(
    val Username: Username,
    val Enabled: Boolean,
    val PreferredMfaSetting: String?,
    val UserCreateDate: Timestamp,
    val UserLastModifiedDate: Timestamp,
    val UserStatus: UserStatus,
    val UserAttributes: List<AttributeType> = emptyList(),
    val MFAOptions: List<MFAOptions>? = null,
    val UserMFASettingList: List<UserMFASetting>? = null
)
