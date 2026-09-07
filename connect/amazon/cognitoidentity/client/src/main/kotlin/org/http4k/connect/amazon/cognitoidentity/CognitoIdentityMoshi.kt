package org.http4k.connect.amazon.cognitoidentity

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.cognitoidentity.model.IdentityId
import org.http4k.connect.amazon.cognitoidentity.model.IdentityPoolId
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object CognitoIdentityMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(CognitoIdentityJsonAdapterFactory)
        .value(AccessKeyId)
        .value(AwsAccount)
        .value(IdentityId)
        .value(IdentityPoolId)
        .value(SecretAccessKey)
        .value(SessionToken)
        .done()
)

@KotshiJsonAdapterFactory
object CognitoIdentityJsonAdapterFactory : JsonAdapter.Factory by KotshiCognitoIdentityJsonAdapterFactory
