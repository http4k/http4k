package org.http4k.connect.amazon.cognito

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.cognito.model.AccessToken
import org.http4k.connect.amazon.cognito.model.AttributeName
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.ClientName
import org.http4k.connect.amazon.cognito.model.ClientSecret
import org.http4k.connect.amazon.cognito.model.CloudFrontDomain
import org.http4k.connect.amazon.cognito.model.ConfirmationCode
import org.http4k.connect.amazon.cognito.model.Destination
import org.http4k.connect.amazon.cognito.model.HeaderName
import org.http4k.connect.amazon.cognito.model.HeaderValue
import org.http4k.connect.amazon.cognito.model.IdToken
import org.http4k.connect.amazon.cognito.model.IpAddress
import org.http4k.connect.amazon.cognito.model.PoolName
import org.http4k.connect.amazon.cognito.model.RefreshToken
import org.http4k.connect.amazon.cognito.model.SecretCode
import org.http4k.connect.amazon.cognito.model.SecretHash
import org.http4k.connect.amazon.cognito.model.ServerName
import org.http4k.connect.amazon.cognito.model.ServerPath
import org.http4k.connect.amazon.cognito.model.Session
import org.http4k.connect.amazon.cognito.model.UserCode
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.amazon.core.model.Password
import org.http4k.connect.amazon.core.model.Username
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object CognitoMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(CognitoJsonAdapterFactory)
        .value(AccessToken)
        .value(AttributeName)
        .value(ClientId)
        .value(ClientName)
        .value(ClientSecret)
        .value(ConfirmationCode)
        .value(Destination)
        .value(HeaderName)
        .value(HeaderValue)
        .value(IdToken)
        .value(IpAddress)
        .value(RefreshToken)
        .value(Password)
        .value(PoolName)
        .value(SecretCode)
        .value(SecretHash)
        .value(ServerName)
        .value(ServerPath)
        .value(Session)
        .value(UserCode)
        .value(Username)
        .value(CloudFrontDomain)
        .value(UserPoolId)
        .done()
)

@KotshiJsonAdapterFactory
object CognitoJsonAdapterFactory : JsonAdapter.Factory by KotshiCognitoJsonAdapterFactory
