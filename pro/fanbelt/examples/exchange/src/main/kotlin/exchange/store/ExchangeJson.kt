package exchange.store

import exchange.domain.OrderId
import exchange.domain.Price
import exchange.domain.Quantity
import exchange.domain.Symbol
import exchange.domain.TradeId
import exchange.domain.VenueDetailsId
import exchange.domain.ViewerId
import org.http4k.fanbelt.withFanbeltMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value

object ExchangeJson : ConfigurableMoshi(
    standardConfig()
        .withFanbeltMappings()
        .value(VenueDetailsId)
        .value(TradeId)
        .value(Symbol)
        .value(Price)
        .value(Quantity)
        .value(OrderId)
        .value(ViewerId)
        .done()
)
