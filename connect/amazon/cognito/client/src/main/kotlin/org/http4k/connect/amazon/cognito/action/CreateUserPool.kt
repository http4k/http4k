package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AccountRecoverySetting
import org.http4k.connect.amazon.cognito.model.AdminCreateUserConfig
import org.http4k.connect.amazon.cognito.model.AliasAttribute
import org.http4k.connect.amazon.cognito.model.AutoVerifiedAttribute
import org.http4k.connect.amazon.cognito.model.DeviceConfiguration
import org.http4k.connect.amazon.cognito.model.EmailConfiguration
import org.http4k.connect.amazon.cognito.model.LambdaConfig
import org.http4k.connect.amazon.cognito.model.MFAConfiguration
import org.http4k.connect.amazon.cognito.model.PoolName
import org.http4k.connect.amazon.cognito.model.SchemaAttributeType
import org.http4k.connect.amazon.cognito.model.SmsConfigurationType
import org.http4k.connect.amazon.cognito.model.UserPoolAddOns
import org.http4k.connect.amazon.cognito.model.UserPoolPolicy
import org.http4k.connect.amazon.cognito.model.UserPoolType
import org.http4k.connect.amazon.cognito.model.UsernameAttribute
import org.http4k.connect.amazon.cognito.model.UsernameConfigurationType
import org.http4k.connect.amazon.cognito.model.VerificationMessageTemplate
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateUserPool(
    val PoolName: PoolName,
    val MfaConfiguration: MFAConfiguration? = null,
    val AccountRecoverySetting: AccountRecoverySetting? = null,
    val AdminCreateUserConfig: AdminCreateUserConfig? = null,
    val AliasAttributes: List<AliasAttribute>? = null,
    val AutoVerifiedAttributes: List<AutoVerifiedAttribute>? = null,
    val DeviceConfiguration: DeviceConfiguration? = null,
    val EmailConfiguration: EmailConfiguration? = null,
    val EmailVerificationMessage: String? = null,
    val EmailVerificationSubject: String? = null,
    val LambdaConfig: LambdaConfig? = null,
    val Policies: UserPoolPolicy? = null,
    val Schema: List<SchemaAttributeType>? = null,
    val SmsAuthenticationMessage: String? = null,
    val SmsConfiguration: SmsConfigurationType? = null,
    val SmsVerificationMessage: String? = null,
    val UsernameAttributes: List<UsernameAttribute>? = null,
    val UsernameConfiguration: UsernameConfigurationType? = null,
    val UserPoolAddOns: UserPoolAddOns? = null,
    val UserPoolTags: Map<String, String>? = null,
    val VerificationMessageTemplate: VerificationMessageTemplate? = null
) : CognitoAction<CreatedUserPool>(CreatedUserPool::class)

@JsonSerializable
data class CreatedUserPool(
    val UserPool: UserPoolType
)
