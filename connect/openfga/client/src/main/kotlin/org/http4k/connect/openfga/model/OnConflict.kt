package org.http4k.connect.openfga.model

import com.squareup.moshi.Json

enum class OnConflict {
    @Json(name = "error")
    ERROR,

    @Json(name = "ignore")
    IGNORE
}
