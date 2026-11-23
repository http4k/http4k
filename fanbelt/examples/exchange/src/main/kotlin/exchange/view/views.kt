package exchange.view

import exchange.domain.OrderId
import exchange.domain.Price
import exchange.domain.Quantity
import exchange.domain.Symbol
import exchange.domain.ViewerId
import org.http4k.fanbelt.page.PageShell
import org.http4k.template.ViewModel

data class TicketSignals(val price: String = "", val quantity: String = "10", val order: String = "")

data class QuoteView(val symbol: Symbol, val last: String, val change: String, val up: Boolean, val flash: String, val spark: String)

data class TickerView(val quotes: List<QuoteView>) : ViewModel

data class Ranked(val rank: Int, val id: ViewerId, val name: String, val equity: String, val pnl: String, val up: Boolean)

data class LeaderboardView(val traders: List<Ranked>) : ViewModel

data class PositionView(val symbol: Symbol, val shares: Int, val value: String)

data class OpenOrderView(val id: OrderId, val symbol: Symbol, val buy: Boolean, val quantity: Quantity, val price: Price)

data class PortfolioView(val name: String, val cash: String, val equity: String, val positions: List<PositionView>, val orders: List<OpenOrderView>) : ViewModel

data class LevelView(val price: Price, val quantity: Int, val width: Int)

data class BookView(val asks: List<LevelView>, val bids: List<LevelView>, val spread: String, val bidArea: String, val askArea: String) : ViewModel

data class CandleView(val x: Int, val top: Int, val height: Int, val high: Int, val low: Int, val up: Boolean)

data class ChartView(val candles: List<CandleView>, val high: String, val low: String) : ViewModel

data class PrintView(val id: String, val at: String, val price: Price, val quantity: Quantity, val up: Boolean)

data class TapeView(val prints: List<PrintView>) : ViewModel

data class Watcher(val id: ViewerId, val name: String)

data class WatchingView(val watchers: List<Watcher>) : ViewModel

data class VenuePageView(val shell: PageShell, val viewer: ViewerId) : ViewModel

data class SymbolPageView(val shell: PageShell, val symbol: Symbol, val viewer: ViewerId) : ViewModel