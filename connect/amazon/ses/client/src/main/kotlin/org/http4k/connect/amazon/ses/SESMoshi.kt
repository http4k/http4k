package org.http4k.connect.amazon.ses

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.RawMessageBase64
import org.http4k.connect.amazon.ses.model.SESMessageId
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import org.http4k.lens.BiDiMapping
import se.ansman.kotshi.KotshiJsonAdapterFactory
import java.nio.charset.Charset

object SESMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(SesJsonAdapterFactory)
        .value(EmailAddress)
        .value(SESMessageId)
        .value(RawMessageBase64)
        .text(BiDiMapping(Charset::class.java, Charset::forName, Charset::name))
        .done()
)

@KotshiJsonAdapterFactory
object SesJsonAdapterFactory : JsonAdapter.Factory by KotshiSesJsonAdapterFactory
