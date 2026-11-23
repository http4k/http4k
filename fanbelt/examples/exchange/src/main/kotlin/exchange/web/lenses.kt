package exchange.web

import exchange.domain.OrderId
import exchange.domain.Side
import exchange.domain.Symbol
import org.http4k.lens.Path
import org.http4k.lens.value

val symbolPath = Path.value(Symbol).of("symbol")
val orderPath = Path.value(OrderId).of("order")
val sidePath = Path.map { Side.valueOf(it.replaceFirstChar(Char::uppercase)) }.of("side")