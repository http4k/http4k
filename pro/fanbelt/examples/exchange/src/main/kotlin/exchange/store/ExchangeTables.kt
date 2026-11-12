package exchange.store

import exchange.domain.Account
import exchange.domain.Mark
import exchange.domain.Order
import exchange.domain.OrderId
import exchange.domain.Symbol
import exchange.domain.Trade
import exchange.domain.VenueDetails
import exchange.domain.ViewerId
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke

class ExchangeTables(tables: Tables) {
    val orders = tables<OrderId, Order>(ORDERS)
    val trades = tables<String, Trade>(TRADES)
    val accounts = tables<ViewerId, Account>(ACCOUNTS)
    val marks = tables<Symbol, Mark>(MARKS)
    val details = tables<String, VenueDetails>(VENUES)

    companion object {
        val ORDERS = TableName.of("orders")
        val TRADES = TableName.of("trades")
        val ACCOUNTS = TableName.of("accounts")
        val MARKS = TableName.of("marks")
        val VENUES = TableName.of("venue")
        val all = listOf(ORDERS, TRADES, ACCOUNTS, MARKS, VENUES)
    }
}