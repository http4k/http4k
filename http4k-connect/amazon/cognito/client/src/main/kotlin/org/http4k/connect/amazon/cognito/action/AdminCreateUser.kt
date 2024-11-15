package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AttributeType
import org.http4k.connect.amazon.cognito.model.DeliveryMedium
import org.http4k.connect.amazon.cognito.model.MessageAction
import org.http4k.connect.amazon.cognito.model.User
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.amazon.core.model.Username
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class AdminCreateUser(
    val Username: Username,
    val UserPoolId: UserPoolId,
    val UserAttributes: List<AttributeType>? = null,
    val DesiredDeliveryMediums: List<DeliveryMedium>? = null,
    val MessageAction: MessageAction? = null,
    val TemporaryPassword: String? = null,
    val ClientMetadata: Map<String, String>? = null,
    val ForceAliasCreation: Boolean? = null,
    val ValidationData: List<AttributeType>? = null
) : CognitoAction<CreatedUser>(CreatedUser::class)

@JsonSerializable
data class CreatedUser(val User: User)
