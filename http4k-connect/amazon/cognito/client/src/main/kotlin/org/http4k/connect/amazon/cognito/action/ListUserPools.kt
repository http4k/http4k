package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.PoolName
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListUserPools(val MaxResults: Int, val NextToken: String? = null) :
    CognitoAction<UserPools>(UserPools::class)

@JsonSerializable
data class CustomEmailSender(
    val LambdaArn: ARN,
    val LambdaVersion: String
)

@JsonSerializable
data class CustomSMSSender(
    val LambdaArn: ARN,
    val LambdaVersion: String
)

@JsonSerializable
data class LambdaConfig(
    val CreateAuthChallenge: String?,
    val CustomEmailSender: CustomEmailSender?,
    val CustomMessage: String?,
    val CustomSMSSender: CustomSMSSender?,
    val DefineAuthChallenge: String?,
    val KMSKeyID: KMSKeyId?,
    val PostAuthentication: String?,
    val PostConfirmation: String?,
    val PreAuthentication: String?,
    val PreSignUp: String?,
    val PreTokenGeneration: String?,
    val UserMigration: String?,
    val VerifyAuthChallengeResponse: String?
)

@JsonSerializable
data class UserPool(
    val CreationDate: Timestamp,
    val Id: UserPoolId,
    val LambdaConfig: LambdaConfig?,
    val LastModifiedDate: Timestamp?,
    val Name: PoolName,
    val Status: String?
)

@JsonSerializable
data class UserPools(
    val NextToken: String?,
    val UserPools: List<UserPool>
)
