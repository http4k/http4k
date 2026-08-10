package org.http4k.connect.amazon.cognito

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.cognito.model.AccountRecoverySetting
import org.http4k.connect.amazon.cognito.model.AdminCreateUserConfig
import org.http4k.connect.amazon.cognito.model.AdvancedSecurityMode.ENFORCED
import org.http4k.connect.amazon.cognito.model.AliasAttribute
import org.http4k.connect.amazon.cognito.model.AnalyticsConfiguration
import org.http4k.connect.amazon.cognito.model.AttributeDataType
import org.http4k.connect.amazon.cognito.model.AttributeType
import org.http4k.connect.amazon.cognito.model.AutoVerifiedAttribute
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.ClientName
import org.http4k.connect.amazon.cognito.model.ClientSecret
import org.http4k.connect.amazon.cognito.model.CustomSender
import org.http4k.connect.amazon.cognito.model.DeliveryMedium.EMAIL
import org.http4k.connect.amazon.cognito.model.DeviceConfiguration
import org.http4k.connect.amazon.cognito.model.EmailConfiguration
import org.http4k.connect.amazon.cognito.model.ExplicitAuthFlow.ALLOW_USER_SRP_AUTH
import org.http4k.connect.amazon.cognito.model.InviteMessageTemplate
import org.http4k.connect.amazon.cognito.model.LambdaConfig
import org.http4k.connect.amazon.cognito.model.MFAConfiguration.OPTIONAL
import org.http4k.connect.amazon.cognito.model.MFAOptions
import org.http4k.connect.amazon.cognito.model.NumberAttributeConstraints
import org.http4k.connect.amazon.cognito.model.PasswordPolicy
import org.http4k.connect.amazon.cognito.model.RecoveryOption
import org.http4k.connect.amazon.cognito.model.RecoveryOptionName.verified_email
import org.http4k.connect.amazon.cognito.model.SchemaAttributeType
import org.http4k.connect.amazon.cognito.model.SmsConfigurationType
import org.http4k.connect.amazon.cognito.model.StringAttributeConstraints
import org.http4k.connect.amazon.cognito.model.TokenValidityUnit.days
import org.http4k.connect.amazon.cognito.model.TokenValidityUnits
import org.http4k.connect.amazon.cognito.model.User
import org.http4k.connect.amazon.cognito.model.UserPoolAddOns
import org.http4k.connect.amazon.cognito.model.UserPoolClient
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.amazon.cognito.model.UserPoolPolicy
import org.http4k.connect.amazon.cognito.model.UserPoolType
import org.http4k.connect.amazon.cognito.model.UserStatus.CONFIRMED
import org.http4k.connect.amazon.cognito.model.UsernameAttribute
import org.http4k.connect.amazon.cognito.model.UsernameConfigurationType
import org.http4k.connect.amazon.cognito.model.VerificationMessageTemplate
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Username
import org.http4k.connect.model.Timestamp
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class CognitoMoshiTest {

    @Test
    fun `fully populated user pool survives a JSON round trip`() {
        val pool = UserPoolType(
            AccountRecoverySetting = AccountRecoverySetting(listOf(RecoveryOption(verified_email, 1))),
            AdminCreateUserConfig = AdminCreateUserConfig(
                true, InviteMessageTemplate("message", "subject", "sms"), 7
            ),
            AliasAttributes = listOf(AliasAttribute.email, AliasAttribute.phone_number),
            Arn = arn,
            AutoVerifiedAttributes = listOf(AutoVerifiedAttribute.email),
            CreationDate = timestamp,
            CustomDomain = "custom.example.com",
            DeviceConfiguration = DeviceConfiguration(true, false),
            Domain = "example",
            EmailConfiguration = EmailConfiguration("config-set", "COGNITO_DEFAULT", "from", "reply-to", arn),
            EmailConfigurationFailure = "none",
            EmailVerificationMessage = "verify",
            EmailVerificationSubject = "verification",
            EstimatedNumberOfUsers = 42,
            Id = UserPoolId.of("ldn-north-1_abcdef"),
            LambdaConfig = LambdaConfig(
                CreateAuthChallenge = "create",
                CustomEmailSender = CustomSender(arn, "V1_0"),
                CustomMessage = "custom"
            ),
            LastModifiedDate = timestamp,
            MfaConfiguration = OPTIONAL,
            Name = "a-pool",
            Policies = UserPoolPolicy(PasswordPolicy(8, true, true, true, true, 7)),
            SchemaAttributes = listOf(
                SchemaAttributeType(
                    AttributeDataType.String, false, true, "email",
                    NumberAttributeConstraints("10", "1"), true, StringAttributeConstraints("100", "1")
                )
            ),
            SmsAuthenticationMessage = "sms auth",
            SmsConfiguration = SmsConfigurationType("external-id", arn),
            SmsConfigurationFailure = "none",
            SmsVerificationMessage = "sms verify",
            Status = "Enabled",
            UsernameAttributes = listOf(UsernameAttribute.email),
            UsernameConfiguration = UsernameConfigurationType(true),
            UserPoolAddOns = UserPoolAddOns(ENFORCED),
            UserPoolTags = mapOf("key" to "value"),
            VerificationMessageTemplate = VerificationMessageTemplate(
                "CONFIRM_WITH_CODE", "message", "by link", "subject", "subject by link", "sms"
            )
        )

        assertThat(CognitoMoshi.asA<UserPoolType>(CognitoMoshi.asFormatString(pool)), equalTo(pool))
    }

    @Test
    fun `fully populated user pool client survives a JSON round trip`() {
        val client = UserPoolClient(
            ClientId = ClientId.of("a-client-id"),
            ClientName = ClientName.of("a-client"),
            UserPoolId = UserPoolId.of("ldn-north-1_abcdef"),
            CreationDate = timestamp,
            LastModifiedDate = timestamp,
            RefreshTokenValidity = 30,
            TokenValidityUnits = TokenValidityUnits(days, days, days),
            AllowedOAuthFlowsUserPoolClient = true,
            ClientSecret = ClientSecret.of("a-secret"),
            AccessTokenValidity = 1,
            AllowedOAuthScopes = listOf("openid", "email"),
            AnalyticsConfiguration = AnalyticsConfiguration(arn, "app-id", "external-id", arn, true),
            CallbackURLs = listOf(Uri.of("https://example.com/callback")),
            DefaultRedirectURI = Uri.of("https://example.com/callback"),
            ExplicitAuthFlows = listOf(ALLOW_USER_SRP_AUTH),
            IdTokenValidity = 1,
            LogoutURLs = listOf(Uri.of("https://example.com/logout")),
            PreventUserExistenceErrors = "ENABLED",
            ReadAttributes = listOf("email"),
            SupportedIdentityProviders = listOf("COGNITO"),
            WriteAttributes = listOf("email")
        )

        assertThat(CognitoMoshi.asA<UserPoolClient>(CognitoMoshi.asFormatString(client)), equalTo(client))
    }

    @Test
    fun `fully populated user survives a JSON round trip`() {
        val user = User(
            Username = Username.of("a-user"),
            UserStatus = CONFIRMED,
            Enabled = true,
            Attributes = listOf(AttributeType("email", "user@example.com")),
            MFAOptions = listOf(MFAOptions("email", EMAIL)),
            UserCreateDate = timestamp,
            UserLastModifiedDate = timestamp
        )

        assertThat(CognitoMoshi.asA<User>(CognitoMoshi.asFormatString(user)), equalTo(user))
    }

    private val arn = ARN.of("arn:partition:kms:ldn-north-1:001234567890:key:foobar")
    private val timestamp = Timestamp.of(1234567890L)
}
