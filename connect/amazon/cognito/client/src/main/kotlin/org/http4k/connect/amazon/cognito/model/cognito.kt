package org.http4k.connect.amazon.cognito.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex
import org.http4k.connect.amazon.cognito.model.TokenValidityUnit.hours
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.core.model.ResourceId
import org.http4k.connect.amazon.core.model.Username
import org.http4k.connect.model.Timestamp
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable


class PoolName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PoolName>(::PoolName)
}

class CloudFrontDomain private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<CloudFrontDomain>(::CloudFrontDomain)
}

class UserPoolId private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<UserPoolId>(::UserPoolId)
}

class ClientId private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<ClientId>(::ClientId)
}

class ClientName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ClientName>(::ClientName)
}

class ClientSecret private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ClientSecret>(::ClientSecret)
}

class Session private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Session>(::Session)
}

class AccessToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AccessToken>(::AccessToken)
}

class UserCode private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<UserCode>(::UserCode, "\\d{6}".regex)
}

class IdToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<IdToken>(::IdToken)
}

class RefreshToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<RefreshToken>(::RefreshToken)
}

class SecretCode private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SecretCode>(::SecretCode)
}

class ConfirmationCode private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ConfirmationCode>(::ConfirmationCode)
}

class SecretHash private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SecretHash>(::SecretHash)
}

@JsonSerializable
data class NewDeviceMetadata(
    val DeviceGroupKey: String? = null,
    val DeviceKey: String? = null
)

@JsonSerializable
data class AuthenticationResult(
    val AccessToken: AccessToken? = null,
    val ExpiresIn: Int? = null,
    val IdToken: IdToken? = null,
    val NewDeviceMetadata: NewDeviceMetadata? = null,
    val RefreshToken: RefreshToken? = null,
    val TokenType: String? = null
)

enum class VerifyStatus {
    SUCCESS, ERROR
}

enum class ChallengeName {
    SMS_MFA, SOFTWARE_TOKEN_MFA, SELECT_MFA_TYPE, MFA_SETUP, PASSWORD_VERIFIER, CUSTOM_CHALLENGE, DEVICE_SRP_AUTH, DEVICE_PASSWORD_VERIFIER, ADMIN_NO_SRP_AUTH, NEW_PASSWORD_REQUIRED
}

enum class AdvancedSecurityMode {
    OFF, AUDIT, ENFORCED
}

enum class AuthFlow {
    USER_SRP_AUTH, REFRESH_TOKEN_AUTH, REFRESH_TOKEN, CUSTOM_AUTH, ADMIN_NO_SRP_AUTH, USER_PASSWORD_AUTH, ADMIN_USER_PASSWORD_AUTH
}

enum class ExplicitAuthFlow {
    ADMIN_NO_SRP_AUTH, CUSTOM_AUTH_FLOW_ONLY, USER_PASSWORD_AUTH, ALLOW_ADMIN_USER_PASSWORD_AUTH, ALLOW_CUSTOM_AUTH, ALLOW_USER_PASSWORD_AUTH, ALLOW_USER_SRP_AUTH, ALLOW_REFRESH_TOKEN_AUTH
}

enum class MessageAction {
    RESEND, SUPPRESS
}

enum class DeliveryMedium {
    SMS, EMAIL
}

enum class UserStatus {
    UNCONFIRMED, CONFIRMED, ARCHIVED, COMPROMISED, UNKNOWN, RESET_REQUIRED, FORCE_CHANGE_PASSWORD
}

enum class RecoveryOptionName {
    verified_email, verified_phone_number, admin_only
}

enum class MFAConfiguration {
    OFF, ON, OPTIONAL
}

enum class AttributeDataType {
    String, Number, DateTime, Boolean
}

enum class AliasAttribute {
    phone_number, email, preferred_username
}

enum class AutoVerifiedAttribute {
    phone_number, email
}

enum class UsernameAttribute {
    phone_number, email
}

enum class UserMFASetting {
    SMS_MFA, SOFTWARE_TOKEN_MFA
}

enum class OAuthFlow {
    code, implicit, client_credentials
}

@JsonSerializable
data class AttributeType(
    val Name: String,
    val Value: String? = null
)

@JsonSerializable
data class MFAOptions(
    val AttributeName: String? = null,
    val DeliveryMedium: DeliveryMedium? = null
)

@JsonSerializable
data class User(
    val Username: Username? = null,
    val UserStatus: UserStatus? = null,
    val Enabled: Boolean? = null,
    val Attributes: List<AttributeType>? = null,
    val MFAOptions: List<MFAOptions>? = null,
    val UserCreateDate: Timestamp? = null,
    val UserLastModifiedDate: Timestamp? = null
)


@JsonSerializable
data class AnalyticsMetadata(
    val AnalyticsEndpointId: String? = null
)

@JsonSerializable
data class UserContextData(
    val EncodedData: String? = null
)

@JsonSerializable
data class RecoveryOption(
    val Name: RecoveryOptionName,
    val Priority: Int
)

@JsonSerializable
data class AccountRecoverySetting(
    val RecoveryMechanisms: List<RecoveryOption>? = null
)

@JsonSerializable
data class InviteMessageTemplate(
    val EmailMessage: String? = null,
    val EmailSubject: String? = null,
    val SMSMessage: String? = null
)

@JsonSerializable
data class AdminCreateUserConfig(
    val AllowAdminCreateUserOnly: Boolean? = null,
    val InviteMessageTemplate: InviteMessageTemplate? = null,
    val UnusedAccountValidityDays: Int? = null
)

@JsonSerializable
data class DeviceConfiguration(
    val ChallengeRequiredOnNewDevice: Boolean? = null,
    val DeviceOnlyRememberedOnUserPrompt: Boolean? = null
)

@JsonSerializable
data class EmailConfiguration(
    val ConfigurationSet: String? = null,
    val EmailSendingAccount: String? = null,
    val From: String? = null,
    val ReplyToEmailAddress: String? = null,
    val SourceArn: ARN? = null
)

@JsonSerializable
data class CustomSender(
    val LambdaArn: ARN? = null,
    val LambdaVersion: String? = null
)

@JsonSerializable
data class LambdaConfig(
    val CreateAuthChallenge: String? = null,
    val CustomEmailSender: CustomSender? = null,
    val CustomMessage: String? = null,
    val CustomSMSSender: CustomSender? = null,
    val DefineAuthChallenge: String? = null,
    val KMSKeyID: KMSKeyId? = null,
    val PostAuthentication: String? = null,
    val PostConfirmation: String? = null,
    val PreAuthentication: String? = null,
    val PreSignUp: String? = null,
    val PreTokenGeneration: String? = null,
    val UserMigration: String? = null,
    val VerifyAuthChallengeResponse: String? = null
)

@JsonSerializable
data class PasswordPolicy(
    val MinimumLength: Int? = null,
    val RequireLowercase: Boolean? = null,
    val RequireNumbers: Boolean? = null,
    val RequireSymbols: Boolean? = null,
    val RequireUppercase: Boolean? = null,
    val TemporaryPasswordValidityDays: Int? = null
)

@JsonSerializable
data class UserPoolPolicy(
    val PasswordPolicy: PasswordPolicy? = null
)

@JsonSerializable
data class NumberAttributeConstraints(
    val MaxValue: String? = null,
    val MinValue: String? = null
)

@JsonSerializable
data class StringAttributeConstraints(
    val MaxLength: String? = null,
    val MinLength: String? = null
)


@JsonSerializable
data class SchemaAttributeType(
    val AttributeDataType: AttributeDataType? = null,
    val DeveloperOnlyAttribute: Boolean? = null,
    val Mutable: Boolean? = null,
    val Name: String? = null,
    val NumberAttributeConstraints: NumberAttributeConstraints? = null,
    val Required: Boolean? = null,
    val StringAttributeConstraints: StringAttributeConstraints? = null
)

@JsonSerializable
data class SmsConfigurationType(
    val ExternalId: String? = null,
    val SnsCallerArn: ARN? = null
)

@JsonSerializable
data class UsernameConfigurationType(
    val CaseSensitive: Boolean
)

@JsonSerializable
data class UserPoolAddOns(
    val AdvancedSecurityMode: AdvancedSecurityMode
)

@JsonSerializable
data class VerificationMessageTemplate(
    val DefaultEmailOption: String? = null,
    val EmailMessage: String? = null,
    val EmailMessageByLink: String? = null,
    val EmailSubject: String? = null,
    val EmailSubjectByLink: String? = null,
    val SmsMessage: String? = null
)

@JsonSerializable
data class UserPoolType(
    val AccountRecoverySetting: AccountRecoverySetting? = null,
    val AdminCreateUserConfig: AdminCreateUserConfig? = null,
    val AliasAttributes: List<AliasAttribute>? = null,
    val Arn: ARN? = null,
    val AutoVerifiedAttributes: List<AutoVerifiedAttribute>? = null,
    val CreationDate: Timestamp? = null,
    val CustomDomain: String? = null,
    val DeviceConfiguration: DeviceConfiguration? = null,
    val Domain: String? = null,
    val EmailConfiguration: EmailConfiguration? = null,
    val EmailConfigurationFailure: String? = null,
    val EmailVerificationMessage: String? = null,
    val EmailVerificationSubject: String? = null,
    val EstimatedNumberOfUsers: Int? = null,
    val Id: UserPoolId? = null,
    val LambdaConfig: LambdaConfig? = null,
    val LastModifiedDate: Timestamp? = null,
    val MfaConfiguration: MFAConfiguration? = null,
    val Name: String? = null,
    val Policies: UserPoolPolicy? = null,
    val SchemaAttributes: List<SchemaAttributeType>? = null,
    val SmsAuthenticationMessage: String? = null,
    val SmsConfiguration: SmsConfigurationType? = null,
    val SmsConfigurationFailure: String? = null,
    val SmsVerificationMessage: String? = null,
    val Status: String? = null,
    val UsernameAttributes: List<UsernameAttribute>? = null,
    val UsernameConfiguration: UsernameConfigurationType? = null,
    val UserPoolAddOns: UserPoolAddOns? = null,
    val UserPoolTags: Map<String, String>? = null,
    val VerificationMessageTemplate: VerificationMessageTemplate? = null
)

@JsonSerializable
data class AnalyticsConfiguration(
    val ApplicationArn: ARN? = null,
    val ApplicationId: String? = null,
    val ExternalId: String? = null,
    val RoleArn: ARN? = null,
    val UserDataShared: Boolean? = null
)

@JsonSerializable
data class TokenValidityUnits(
    val AccessToken: TokenValidityUnit = hours,
    val IdToken: TokenValidityUnit = hours,
    val RefreshToken: TokenValidityUnit = hours
)

enum class TokenValidityUnit {
    days, hours, minutes, seconds
}

@JsonSerializable
data class UserPoolClient(
    val ClientId: ClientId,
    val ClientName: ClientName,
    val UserPoolId: UserPoolId,
    val CreationDate: Timestamp,
    val LastModifiedDate: Timestamp,
    val RefreshTokenValidity: Int,
    val TokenValidityUnits: TokenValidityUnits,
    val AllowedOAuthFlowsUserPoolClient: Boolean? = null,
    val AllowedOAuthFlows: List<OAuthFlow>? = null,
    val ClientSecret: ClientSecret? = null,
    val AccessTokenValidity: Int? = null,
    val AllowedOAuthScopes: List<String>? = null,
    val AnalyticsConfiguration: AnalyticsConfiguration? = null,
    val CallbackURLs: List<Uri>? = null,
    val DefaultRedirectURI: Uri? = null,
    val ExplicitAuthFlows: List<ExplicitAuthFlow>? = null,
    val IdTokenValidity: Int? = null,
    val LogoutURLs: List<Uri>? = null,
    val PreventUserExistenceErrors: String? = null,
    val ReadAttributes: List<String>? = null,
    val SupportedIdentityProviders: List<String>? = null,
    val WriteAttributes: List<String>? = null
)

@JsonSerializable
data class Jwk(
    val e: String,
    val kid: String,
    val n: String,
    val alg: String = "RS256",
    val kty: String = "RSA",
    val use: String = "sig"
)

@JsonSerializable
data class Jwks(val keys: List<Jwk>)

class Destination private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Destination>(::Destination)
}

class AttributeName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AttributeName>(::AttributeName)
}

class HeaderName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<HeaderName>(::HeaderName)
}

class HeaderValue private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<HeaderValue>(::HeaderValue)
}

class IpAddress private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<IpAddress>(::IpAddress)
}

class ServerName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ServerName>(::ServerName)
}

class ServerPath private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ServerPath>(::ServerPath)
}

@JsonSerializable
data class CodeDeliveryDetails(
    val AttributeName: AttributeName?,
    val DeliveryMedium: DeliveryMedium?,
    val Destination: Destination?
)

@JsonSerializable
data class ContextData(
    val HttpHeaders: List<HttpHeader>,
    val IpAddress: IpAddress,
    val ServerName: ServerName,
    val ServerPath: ServerPath,
    val EncodedData: String? = null
)

@JsonSerializable
data class HttpHeader(
    val headerName: HeaderName? = null,
    val headerValue: HeaderValue? = null
)
