package org.http4k.connect.amazon.iamidentitycenter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.iamidentitycenter.model.AccessToken
import org.http4k.connect.amazon.iamidentitycenter.model.ClientId
import org.http4k.connect.amazon.iamidentitycenter.model.ClientName
import org.http4k.connect.amazon.iamidentitycenter.model.ClientSecret
import org.http4k.connect.amazon.iamidentitycenter.model.DeviceCode
import org.http4k.connect.amazon.iamidentitycenter.model.IdToken
import org.http4k.connect.amazon.iamidentitycenter.model.RefreshToken
import org.http4k.connect.amazon.iamidentitycenter.model.RoleName
import org.http4k.connect.amazon.iamidentitycenter.model.SessionId
import org.http4k.connect.amazon.iamidentitycenter.model.UserCode
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object IAMIdentityCenterMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(IAMIdentityCenterJsonAdapterFactory)
        .add(AwsCoreJsonAdapterFactory())
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
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
        .withAwsCoreMappings()
        .done()
)

@KotshiJsonAdapterFactory
object IAMIdentityCenterJsonAdapterFactory : JsonAdapter.Factory by KotshiIAMIdentityCenterJsonAdapterFactory
