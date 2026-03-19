package org.http4k.connect.mpp

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.mpp.model.ChallengeId
import org.http4k.connect.mpp.model.Currency
import org.http4k.connect.mpp.model.PaymentAmount
import org.http4k.connect.mpp.model.PaymentIntent
import org.http4k.connect.mpp.model.PaymentMethod
import org.http4k.connect.mpp.model.PaymentReference
import org.http4k.connect.mpp.model.PaymentSource
import org.http4k.connect.mpp.model.Realm
import org.http4k.connect.mpp.model.ReceiptStatus
import org.http4k.connect.mpp.model.Recipient
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.SetAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object MppMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(MppMoshiJsonAdapterFactory)
        .addLast(ListAdapter)
        .addLast(SetAdapter)
        .addLast(MapAdapter)
        .addLast(ThrowableAdapter)
        .addLast(MoshiNodeAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withMppMappings()
        .done()
)

fun <T> AutoMappingConfiguration<T>.withMppMappings() = apply {
    value(ChallengeId)
    value(Currency)
    value(PaymentAmount)
    value(PaymentIntent)
    value(PaymentMethod)
    value(PaymentReference)
    value(PaymentSource)
    value(Realm)
    value(ReceiptStatus)
    value(Recipient)
}

@KotshiJsonAdapterFactory
object MppMoshiJsonAdapterFactory : JsonAdapter.Factory by KotshiMppMoshiJsonAdapterFactory
