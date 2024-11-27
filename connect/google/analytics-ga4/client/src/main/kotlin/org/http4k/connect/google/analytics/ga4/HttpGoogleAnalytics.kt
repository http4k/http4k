package org.http4k.connect.google.analytics.ga4

import org.http4k.client.JavaHttpClient
import org.http4k.connect.google.analytics.ga4.model.ApiSecret
import org.http4k.connect.google.analytics.ga4.model.MeasurementId
import org.http4k.connect.google.analytics.model.GOOGLE_ANALYTICS_URL
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.lens.Query
import org.http4k.lens.value

fun GoogleAnalytics.Companion.Http(
    measurementId: MeasurementId,
    apiSecret: ApiSecret,
    rawHttp: HttpHandler = JavaHttpClient()
) =
    object : GoogleAnalytics {
        private val http = SetBaseUriFrom(GOOGLE_ANALYTICS_URL).then(rawHttp)

        override fun <R> invoke(action: GoogleAnalyticsAction<R>) = action.toResult(
            http(
                action.toRequest()
                    .with(measurementIdQuery of measurementId)
                    .with(apiSecretQuery of apiSecret)
            )
        )
    }

val measurementIdQuery = Query.value(MeasurementId).required("measurement_id")
val apiSecretQuery = Query.value(ApiSecret).required("api_secret")
