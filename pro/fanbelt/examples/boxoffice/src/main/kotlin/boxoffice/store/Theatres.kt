package boxoffice.store

import boxoffice.domain.Layout
import boxoffice.domain.Show
import boxoffice.domain.ShowId
import boxoffice.domain.ShowTitle
import boxoffice.domain.Theatre
import dev.forkhandles.values.random
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.everything
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Clock
import java.util.Random

class Theatres(private val storage: DocumentStorage) {
    fun exists(theatre: TenantId) = storage.read { BoxOfficeTables(it.tables(theatre)).theatre.get(Theatre.SELF) != null }

    fun all() = storage.read { it.having(BoxOfficeTables.THEATRES, everything<Theatre>()) }.toList()

    fun create(theatre: TenantId, clock: Clock, random: Random, vararg shows: Pair<ShowTitle, Layout>) = storage.write { tenants ->
        val tables = BoxOfficeTables(tenants.tables(theatre))
        tables.theatre.insert(Theatre(clock.instant()))
        shows.forEach { (title, layout) -> tables.shows.insert(Show(ShowId.random(random), title, layout)) }
    }
}