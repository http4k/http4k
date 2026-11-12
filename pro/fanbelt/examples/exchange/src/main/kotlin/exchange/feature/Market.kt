package exchange.feature

import dev.forkhandles.values.random
import exchange.domain.Account
import exchange.domain.Book
import exchange.domain.Holdings
import exchange.domain.Level
import exchange.domain.Mark
import exchange.domain.Order
import exchange.domain.OrderId
import exchange.domain.Price
import exchange.domain.Quantity
import exchange.domain.Side
import exchange.domain.Standings
import exchange.domain.Symbol
import exchange.domain.Ticket
import exchange.domain.Trade
import exchange.domain.ViewerId
import exchange.store.ExchangeTables
import org.http4k.fanbelt.store.Order.Newest
import org.http4k.fanbelt.store.eq
import java.time.Clock
import java.util.Random
import kotlin.math.min

class Market(private val tables: ExchangeTables, private val clock: Clock, private val random: Random) {
    fun book(symbol: Symbol) = Book(levels(symbol, Side.Buy), levels(symbol, Side.Sell))

    fun trades(symbol: Symbol) = tables.trades.page(Trade::symbol eq symbol, Newest, limit = 200).items

    fun holdings(trader: ViewerId) = Holdings(account(trader), tables.orders.page(Order::trader eq trader).items)

    fun standings() = Standings(tables.accounts.page().items, tables.orders.page().items)

    fun marks() = tables.marks.page().items.associateBy { it.id }

    fun marketPrice(symbol: Symbol, side: Side): Price? {
        val reference = tables.marks.get(symbol)?.price ?: resting(symbol, side.opposite).firstOrNull()?.price ?: return null
        return Price.cents(reference.value.toLong() * (if (side == Side.Buy) 105 else 95) / 100)
    }

    fun affords(trader: ViewerId, ticket: Ticket) = when (ticket.side) {
        Side.Buy -> account(trader).cash >= ticket.price.value.toLong() * ticket.quantity.value
        Side.Sell -> account(trader).held(ticket.symbol) >= ticket.quantity.value
    }

    fun place(trader: ViewerId, ticket: Ticket, immediate: Boolean): List<Trade> {
        val order = Order(OrderId.random(random), ticket.symbol, trader, ticket.side, ticket.price, ticket.quantity, clock.instant())
        change(trader) { it.reserving(order, 1) }
        val (left, trades) = match(order, order.quantity.value, resting(order.symbol, order.side.opposite), emptyList())
        when {
            left == 0 -> Unit
            immediate -> change(trader) { it.reserving(order.copy(quantity = Quantity.of(left)), -1) }
            else -> tables.orders.insert(order.copy(quantity = Quantity.of(left)))
        }
        return trades
    }

    fun order(id: OrderId) = tables.orders.get(id)

    fun cancel(order: Order) {
        tables.orders.delete(order.id)
        change(order.trader) { it.reserving(order, -1) }
    }

    fun record(prices: Map<Symbol, Price>): Boolean {
        val moved = prices.filter { (symbol, price) -> tables.marks.get(symbol)?.price != price }
        moved.forEach { (symbol, price) ->
            when (tables.marks.get(symbol)) {
                null -> tables.marks.insert(Mark(symbol, price, listOf(price.value)))
                else -> tables.marks.update(symbol) { it.copy(price = price, history = (it.history + price.value).takeLast(60)) }
            }
        }
        return moved.isNotEmpty()
    }

    private fun levels(symbol: Symbol, side: Side) =
        resting(symbol, side).groupBy { it.price }.map { (price, orders) -> Level(price, orders.sumOf { it.quantity.value }) }

    private fun resting(symbol: Symbol, side: Side) = side.best(tables.orders.page((Order::symbol eq symbol) and (Order::side eq side)).items)

    private fun account(trader: ViewerId) = tables.accounts.get(trader) ?: Account.opening(trader)

    private fun change(trader: ViewerId, change: (Account) -> Account) = when (tables.accounts.get(trader)) {
        null -> tables.accounts.insert(change(Account.opening(trader)))
        else -> tables.accounts.update(trader, change)
    }

    private fun Account.reserving(order: Order, direction: Int) = when (order.side) {
        Side.Buy -> plus(cash = -direction * order.reserved)
        Side.Sell -> plus(symbol = order.symbol, shares = -direction * order.quantity.value)
    }

    private tailrec fun match(taker: Order, left: Int, makers: List<Order>, trades: List<Trade>): Pair<Int, List<Trade>> {
        val maker = makers.firstOrNull()
        if (left == 0 || maker == null || !taker.side.crosses(taker.price, maker.price)) return left to trades
        val filled = min(left, maker.quantity.value)
        return match(taker, left - filled, makers.drop(1), trades + fill(taker, maker, filled))
    }

    private fun fill(taker: Order, maker: Order, quantity: Int): Trade {
        val (buy, sell) = if (taker.side == Side.Buy) taker to maker else maker to taker
        val trade = Trade(random.nextLong().toString(), maker.symbol, maker.price, Quantity.of(quantity), buy.trader, sell.trader, clock.instant())
        if (quantity == maker.quantity.value) tables.orders.delete(maker.id) else tables.orders.update(maker.id) { it.copy(quantity = Quantity.of(it.quantity.value - quantity)) }
        change(buy.trader) { it.plus(cash = (buy.price.value - maker.price.value).toLong() * quantity, symbol = maker.symbol, shares = quantity) }
        change(sell.trader) { it.plus(cash = maker.price.value.toLong() * quantity) }
        tables.trades.insert(trade)
        return trade
    }
}