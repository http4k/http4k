package exchange.store

import exchange.domain.VenueDetails
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.everything
import org.http4k.fanbelt.tenancy.TenantId

class Venues(private val storage: DocumentStorage) {
    fun all(): List<TenantId> = storage.read { it.having(ExchangeTables.VENUES, everything<VenueDetails>()) }.sortedBy { it.value }

    fun exists(venue: TenantId) = storage.read { ExchangeTables(it.tables(venue)).details.get(VenueDetails.SELF) != null }

    fun create(venue: TenantId, record: VenueDetails) = storage.write { tenants ->
        val records = ExchangeTables(tenants.tables(venue)).details
        if (records.get(VenueDetails.SELF) == null) records.insert(record)
    }
}