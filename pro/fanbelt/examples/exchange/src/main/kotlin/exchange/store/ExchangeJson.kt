package exchange.store

import exchange.domain.OrderId
import exchange.domain.Price
import exchange.domain.Quantity
import exchange.domain.Symbol
import exchange.domain.ViewerId
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import org.http4k.fanbelt.withFanbeltMappings

object ExchangeJson : ConfigurableMoshi(
    standardConfig()
        .withFanbeltMappings()
        .value(Symbol)
        .value(Price)
        .value(Quantity)
        .value(OrderId)
        .value(ViewerId)
        .done()
)