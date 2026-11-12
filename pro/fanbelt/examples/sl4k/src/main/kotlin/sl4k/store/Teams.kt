package sl4k.store

import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import sl4k.domain.Channel
import sl4k.domain.ChannelName
import sl4k.domain.Team
import java.time.Clock

class Teams(private val storage: DocumentStorage) {
    fun exists(team: TenantId) = storage.read { ChatTables(it.tables(team)).team.get(Team.SELF) != null }

    fun create(team: TenantId, clock: Clock, vararg channels: ChannelName) = storage.write { tenants ->
        val tables = ChatTables(tenants.tables(team))
        tables.team.insert(Team(clock.instant()))
        channels.forEach { tables.channels.insert(Channel(it, clock.instant())) }
    }
}