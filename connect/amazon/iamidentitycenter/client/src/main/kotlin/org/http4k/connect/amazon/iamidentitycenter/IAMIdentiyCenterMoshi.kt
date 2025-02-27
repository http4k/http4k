package org.http4k.connect.amazon.iamidentitycenter

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.iamidentitycenter.model.AccessToken
import org.http4k.connect.amazon.iamidentitycenter.model.AuthCode
import org.http4k.connect.amazon.iamidentitycenter.model.ClientId
import org.http4k.connect.amazon.iamidentitycenter.model.ClientName
import org.http4k.connect.amazon.iamidentitycenter.model.ClientSecret
import org.http4k.connect.amazon.iamidentitycenter.model.DeviceCode
import org.http4k.connect.amazon.iamidentitycenter.model.GrantType
import org.http4k.connect.amazon.iamidentitycenter.model.IdToken
import org.http4k.connect.amazon.iamidentitycenter.model.PKCECodeVerifier
import org.http4k.connect.amazon.iamidentitycenter.model.RefreshToken
import org.http4k.connect.amazon.iamidentitycenter.model.RoleName
import org.http4k.connect.amazon.iamidentitycenter.model.SessionId
import org.http4k.connect.amazon.iamidentitycenter.model.UserCode
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import org.http4k.lens.BiDiMapping
import se.ansman.kotshi.KotshiJsonAdapterFactory

object IAMIdentityCenterMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(IAMIdentityCenterJsonAdapterFactory)
        .value(AccessToken)
        .value(ClientName)
        .value(ClientId)
        .value(ClientSecret)
        .value(DeviceCode)
        .value(IdToken)
        .value(RefreshToken)
        .value(SessionId)
        .value(RoleName)
        .value(UserCode)
        .value(AuthCode)
        .value(PKCECodeVerifier)
        .value(PKCECodeVerifier)
        .text(BiDiMapping(GrantType::class.java, GrantType::fromWire, GrantType::wireValue))
        .done()
)

@KotshiJsonAdapterFactory
object IAMIdentityCenterJsonAdapterFactory : JsonAdapter.Factory by KotshiIAMIdentityCenterJsonAdapterFactory
