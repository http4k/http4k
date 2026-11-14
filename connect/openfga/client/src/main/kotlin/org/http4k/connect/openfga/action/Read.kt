package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.OpenFgaMoshi.json
import org.http4k.connect.openfga.model.ConsistencyPreference
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TupleFilter
import org.http4k.connect.openfga.model.Tuples
import org.http4k.core.Method.POST
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class Read(
    val store: StoreId,
    val filter: TupleFilter? = null,
    val consistency: ConsistencyPreference? = null
) : OpenFgaAction<Tuples>(kClass()) {
    override fun toRequest() = Request(POST, "/stores/$store/read").json(ReadBody(filter, consistency))
}

@JsonSerializable
data class ReadBody(val tuple_key: TupleFilter? = null, val consistency: ConsistencyPreference? = null)
