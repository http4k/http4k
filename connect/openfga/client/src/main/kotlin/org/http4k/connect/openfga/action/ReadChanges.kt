package org.http4k.connect.openfga.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.openfga.OpenFgaAction
import org.http4k.connect.openfga.model.ContinuationToken
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.TupleChanges
import org.http4k.connect.openfga.model.TypeName
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Query
import org.http4k.lens.value

@Http4kConnectAction
data class ReadChanges(
    val store: StoreId,
    val continuationToken: ContinuationToken? = null,
    val type: TypeName? = null
) : OpenFgaAction<TupleChanges>(kClass()) {
    override fun toRequest() = Request(GET, "/stores/$store/changes")
        .with(continuationTokenQuery of continuationToken, typeQuery of type)
}

private val continuationTokenQuery = Query.value(ContinuationToken).optional("continuation_token")

private val typeQuery = Query.value(TypeName).optional("type")
