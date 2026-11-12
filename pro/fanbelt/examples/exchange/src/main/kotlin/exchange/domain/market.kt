package exchange.domain

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.minValue
import org.http4k.fanbelt.store.Identified
import java.time.Instant

val SYMBOLS = listOf("ORN", "HTK", "DSTR", "KTLN", "JVM")

class Symbol private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<Symbol>(::Symbol, { it in SYMBOLS })
}

fun decimal(cents: Int) = "%d.%02d".format(cents / 100, cents % 100)

val symbols = SYMBOLS.map(Symbol::of)

class Price private constructor(cents: Int) : IntValue(cents) {
    override fun toString() = decimal(value)

    companion object : ValueFactory<Price, Int>(::Price, 1.minValue, { (it.trim().toBigDecimal() * 100.toBigDecimal()).intValueExact() }, { decimal(it) }) {
        fun cents(cents: Long) = of(cents.toInt())
    }
}

class Quantity private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<Quantity>(::Quantity, { it in 1..10_000 })
}

class OrderId private constructor(value: java.util.UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<OrderId>(::OrderId)
}

enum class Side {
    Buy, Sell;

    val opposite get() = if (this == Buy) Sell else Buy

    fun best(orders: List<Order>) = if (this == Buy) orders.sortedByDescending { it.price.value } else orders.sortedBy { it.price.value }

    fun crosses(limit: Price, resting: Price) = if (this == Buy) limit.value >= resting.value else limit.value <= resting.value
}

data class Order(
    override val id: OrderId,
    val symbol: Symbol,
    val trader: ViewerId,
    val side: Side,
    val price: Price,
    val quantity: Quantity,
    val at: Instant,
) : Identified<OrderId> {
    val reserved get() = price.value.toLong() * quantity.value
}

data class Trade(
    override val id: String,
    val symbol: Symbol,
    val price: Price,
    val quantity: Quantity,
    val buyer: ViewerId,
    val seller: ViewerId,
    val at: Instant,
) : Identified<String>

data class Account(override val id: ViewerId, val cash: Long, val held: Map<String, Int>) : Identified<ViewerId> {
    fun held(symbol: Symbol) = held[symbol.value] ?: 0

    fun plus(cash: Long = 0, symbol: Symbol? = null, shares: Int = 0) =
        copy(cash = this.cash + cash, held = if (symbol == null) held else held + (symbol.value to held(symbol) + shares))

    fun equity(orders: List<Order>, marks: Map<Symbol, Mark>) =
        cash + symbols.sumOf { symbol -> (held(symbol) + orders.filter { it.symbol == symbol && it.side == Side.Sell }.sumOf { it.quantity.value }).toLong() * (marks[symbol]?.price?.value ?: 0) } +
            orders.filter { it.side == Side.Buy }.sumOf { it.reserved }

    companion object {
        fun opening(trader: ViewerId) = Account(trader, 10_000_000, SYMBOLS.associateWith { 100 })
    }
}

data class Mark(override val id: Symbol, val price: Price, val history: List<Int>) : Identified<Symbol>

data class VenueDetails(val createdAt: Instant, override val id: String = SELF) : Identified<String> {
    companion object {
        const val SELF = "self"
    }
}

data class Level(val price: Price, val quantity: Int)

data class Book(val bids: List<Level>, val asks: List<Level>)

data class Holdings(val account: Account, val orders: List<Order>)

data class Standings(val accounts: List<Account>, val orders: List<Order>)

data class Leaderboard(val top: List<Pair<ViewerId, Long>>, val opening: Long)

data class Ticket(val symbol: Symbol, val side: Side, val price: Price, val quantity: Quantity)

sealed interface Actor {
    data class Trader(val id: ViewerId) : Actor
    data object Feed : Actor
}

data class TicketInput(val symbol: Symbol, val side: Side, val price: String, val quantity: String)

data class Quote(val symbol: Symbol, val last: Price?, val open: Price?, val mark: Mark?)