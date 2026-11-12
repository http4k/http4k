package exchange.web

import exchange.domain.Book
import exchange.domain.Level
import exchange.domain.Price
import exchange.domain.Quote
import exchange.domain.Side
import exchange.domain.Trade
import exchange.domain.Viewer
import exchange.domain.decimal
import exchange.domain.symbols
import exchange.feature.MarketFeature
import exchange.view.BookView
import exchange.view.CandleView
import exchange.view.ChartView
import exchange.view.LeaderboardView
import exchange.view.LevelView
import exchange.view.OpenOrderView
import exchange.view.PortfolioView
import exchange.view.PositionView
import exchange.view.PrintView
import exchange.view.QuoteView
import exchange.view.Ranked
import exchange.view.TapeView
import exchange.view.TickerView
import exchange.view.Watcher
import exchange.view.WatchingView
import org.http4k.fanbelt.page.Region
import org.http4k.template.TemplateRenderer

class ExchangeRegions(exchange: MarketFeature, renderer: TemplateRenderer) {
    val ticker = Region("ticker", renderer, exchange.quotes) { quotes, _ -> TickerView(quotes.map(::quoteView)) }
    val leaderboard = Region("leaderboard", renderer, exchange.leaderboard) { (top, opening), _ ->
        LeaderboardView(top.mapIndexed { i, (id, equity) -> Ranked(i + 1, id, Viewer(id).name, money(equity), signed(equity - opening), equity >= opening) })
    }
    val portfolio = Region("portfolio", renderer, { request -> principal(request).id }, exchange.portfolio) { (holdings, marks), request ->
        val (account, orders) = holdings
        PortfolioView(
            principal(request).name, money(account.cash), money(account.equity(orders, marks)),
            symbols.map { symbol -> account.held(symbol) + orders.filter { it.symbol == symbol && it.side == Side.Sell }.sumOf { it.quantity.value } }
                .zip(symbols) { shares, symbol -> PositionView(symbol, shares, money(shares.toLong() * (marks[symbol]?.price?.value ?: 0))) },
            orders.sortedByDescending { it.at }.map { OpenOrderView(it.id, it.symbol, it.side == Side.Buy, it.quantity, it.price) },
        )
    }
    val book = Region("book", renderer, symbolPath, exchange.book) { book, _ -> bookView(book) }
    val chart = Region("chart", renderer, symbolPath, exchange.trades) { trades, _ -> chartView(trades) }
    val tape = Region("tape", renderer, symbolPath, exchange.trades) { trades, _ ->
        TapeView(trades.take(24).zip(trades.drop(1) + trades.takeLast(1)) { trade, before -> PrintView(trade.id, trade.at.toString().substring(11, 19), trade.price, trade.quantity, trade.price.value >= before.price.value) })
    }
    val watching = Region("watching", renderer, symbolPath, exchange.watching) { present, _ -> WatchingView(present.keys.map { Watcher(it, Viewer(it).name) }.sortedBy { it.name }) }
}

private fun quoteView(quote: Quote): QuoteView {
    val last = quote.last ?: quote.mark?.price
    val open = quote.open ?: quote.mark?.history?.firstOrNull()?.let(Price::of)
    val change = if (last == null || open == null) 0.0 else (last.value - open.value) * 100.0 / open.value
    return QuoteView(quote.symbol, last?.toString() ?: "—", "%+.2f%%".format(change), change >= 0, "px-${quote.symbol}-${last?.value}", line(quote.mark?.history.orEmpty(), 80, 22))
}

private fun bookView(book: Book): BookView {
    val asks = book.asks.take(10)
    val bids = book.bids.take(10)
    val most = (asks + bids).maxOfOrNull { it.quantity } ?: 1
    val spread = if (asks.isEmpty() || bids.isEmpty()) "—" else decimal(asks.first().price.value - bids.first().price.value)
    val prices = (bids + asks).map { it.price.value }
    val low = prices.minOrNull() ?: 0
    val span = ((prices.maxOrNull() ?: 1) - low).coerceAtLeast(1)
    val depth = maxOf(bids.sumOf { it.quantity }, asks.sumOf { it.quantity }).coerceAtLeast(1)
    fun x(price: Price) = (price.value - low) * 300 / span
    fun area(levels: List<Level>, edge: Int) = if (levels.isEmpty()) "" else
        "${x(levels.first().price)},100 " + levels.runningFold(0 to 0) { (_, sum), level -> x(level.price) to sum + level.quantity }.drop(1)
            .joinToString(" ") { (at, sum) -> "$at,${100 - sum * 100 / depth}" } + " $edge,100"
    return BookView(
        asks.reversed().map { LevelView(it.price, it.quantity, it.quantity * 100 / most) },
        bids.map { LevelView(it.price, it.quantity, it.quantity * 100 / most) },
        spread, area(bids, 0), area(asks, 300),
    )
}

private fun chartView(trades: List<Trade>): ChartView {
    val buckets = trades.reversed().groupBy { it.at.epochSecond / 5 }.values.toList().takeLast(48)
    val high = buckets.flatten().maxOfOrNull { it.price.value } ?: 1
    val low = buckets.flatten().minOfOrNull { it.price.value } ?: 0
    val span = (high - low).coerceAtLeast(1)
    fun y(price: Int) = 4 + (high - price) * 152 / span
    return ChartView(buckets.mapIndexed { i, bucket ->
        val open = bucket.first().price.value
        val close = bucket.last().price.value
        CandleView((48 - buckets.size + i) * 10, y(maxOf(open, close)), (y(minOf(open, close)) - y(maxOf(open, close))).coerceAtLeast(1), y(bucket.maxOf { it.price.value }), y(bucket.minOf { it.price.value }), close >= open)
    }, if (buckets.isEmpty()) "" else decimal(high), if (buckets.isEmpty()) "" else decimal(low))
}

private fun line(values: List<Int>, width: Int, height: Int): String {
    val low = values.minOrNull() ?: 0
    val span = ((values.maxOrNull() ?: 1) - low).coerceAtLeast(1)
    return values.mapIndexed { i, value -> "${i * width / (values.size - 1).coerceAtLeast(1)},${height - (value - low) * height / span}" }.joinToString(" ")
}

private fun money(cents: Long) = "%,d.%02d".format(cents / 100, cents % 100)

private fun signed(cents: Long) = (if (cents >= 0) "+" else "−") + money(kotlin.math.abs(cents))