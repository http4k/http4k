package org.http4k.connect.x402

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.TransactionHash
import org.http4k.connect.x402.model.WalletAddress
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

object X402Moshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(X402MoshiJsonAdapterFactory)
        .addLast(ListAdapter)
        .addLast(SetAdapter)
        .addLast(MapAdapter)
        .addLast(ThrowableAdapter)
        .addLast(MoshiNodeAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withX402Mappings()
        .done()
)

fun <T> AutoMappingConfiguration<T>.withX402Mappings() = apply {
    value(AssetAddress)
    value(PaymentAmount)
    value(PaymentNetwork)
    value(PaymentScheme)
    value(TransactionHash)
    value(WalletAddress)
}

@KotshiJsonAdapterFactory
object X402MoshiJsonAdapterFactory : JsonAdapter.Factory by KotshiX402MoshiJsonAdapterFactory
