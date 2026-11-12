package exchange.feature

import exchange.domain.Actor
import exchange.domain.Leaderboard
import exchange.domain.OrderId
import exchange.domain.Price
import exchange.domain.Quantity
import exchange.domain.Quote
import exchange.domain.Side
import exchange.domain.Symbol
import exchange.domain.Ticket
import exchange.domain.TicketInput
import exchange.domain.ViewerId
import exchange.domain.symbols
import exchange.store.ExchangeTables
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Access.Allowed
import org.http4k.fanbelt.feature.Access.Denied
import org.http4k.fanbelt.feature.Access.Unavailable
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.topic
import java.time.Clock
import java.util.Random

class MarketFeature(clock: Clock, random: Random) : Feature<Market, Actor>({ tables -> Market(ExchangeTables(tables), clock, random) }) {
    val book by state(Symbol) { market, symbol -> market.book(symbol) }
    val trades by state(Symbol) { market, symbol -> market.trades(symbol) }
    val holdings by state(ViewerId) { market, trader -> market.holdings(trader) }
    val standings by state { it.standings() }
    val marks by state { it.marks() }

    val watching by presence(Symbol, ViewerId)

    val quotes by derived { ctx, _: Unit ->
        val marks = ctx.read(marks)
        symbols.map { symbol ->
            val recent = ctx.read(trades, symbol)
            Quote(symbol, recent.firstOrNull()?.price, recent.lastOrNull()?.price, marks[symbol])
        }
    }
    val portfolio by derived { ctx, trader: ViewerId -> ctx.read(holdings, trader) to ctx.read(marks) }
    val leaderboard by derived { ctx, _: Unit ->
        val (accounts, orders) = ctx.read(standings)
        val marks = ctx.read(marks)
        val top = accounts.map { it.id to it.equity(orders.filter { order -> order.trader == it.id }, marks) }.sortedByDescending { it.second }.take(10)
        Leaderboard(top, opening = 10_000_000 + 100L * marks.values.sumOf { it.price.value })
    }

    val trader by rule { _, actor -> if (actor is Actor.Trader) Allowed(actor) else Denied }

    val feed by rule { _, actor -> if (actor == Actor.Feed) Allowed(actor) else Denied }

    val yourOrder by rule { ctx, actor, id: OrderId ->
        val order = ctx.domain.order(id)
        when {
            actor !is Actor.Trader -> Denied
            order == null -> Unavailable(FieldName.of("order"), "That order has already filled or been cancelled")
            order.trader != actor.id -> Denied
            else -> Allowed(order)
        }
    }

    val place by operation(trader) { market, _, input: TicketInput, actor ->
        val quantity = FieldName.of("quantity").check(Quantity, input.quantity, "Between 1 and 10,000 shares").orElse { return@operation it }
        val price = when {
            input.price.isBlank() -> market.marketPrice(input.symbol, input.side) ?: return@operation Rejected(FieldName.of("price"), "No market yet: set a limit")
            else -> FieldName.of("price").check(Price, input.price, "A price like 101.25").orElse { return@operation it }
        }
        val ticket = Ticket(input.symbol, input.side, price, quantity)
        if (!market.affords(actor.id, ticket)) return@operation Rejected(FieldName.of("quantity"), if (input.side == Side.Buy) "Not enough cash" else "You don't hold that many")
        val fills = market.place(actor.id, ticket, input.price.isBlank())
        val traders = (fills.flatMap { listOf(it.buyer, it.seller) } + actor.id).toSet()
        val tape = if (fills.isEmpty()) emptySet() else setOf(trades.topic(input.symbol))
        Changes.of(setOf(book.topic(input.symbol), standings.topic()) + traders.map { holdings.topic(it) } + tape)
    }

    val cancel by operation(yourOrder) { market, _, _, order ->
        market.cancel(order)
        Changes.of(book.topic(order.symbol), holdings.topic(order.trader), standings.topic())
    }

    val mark by operation(feed) { market, _, prices: Map<Symbol, Price>, _ ->
        if (market.record(prices)) Changes.of(marks.topic()) else Changes.none()
    }
}