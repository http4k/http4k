package org.http4k.wiretap.traffic

import org.http4k.datastar.MorphMode.prepend
import org.http4k.datastar.Selector
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.sendPatchElements
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.toSummary

fun TrafficStream(transactions: TransactionStore, elements: DatastarElementRenderer) =
    "/stream" bind sse { sse ->
        val unsubscribe = transactions.subscribe { transaction ->
            sse.sendPatchElements(
                elements(TransactionRowView(transaction.toSummary())),
                prepend,
                Selector.of("#tx-list")
            )
        }
        sse.onClose { unsubscribe() }
    }
