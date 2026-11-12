package board.store

import board.domain.Team
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.everything
import org.http4k.fanbelt.tenancy.TenantId

class Teams(private val storage: DocumentStorage) {
    fun all(): List<TenantId> = storage.read { it.having(BoardTables.TEAMS, everything<Team>()) }.sortedBy { it.value }

    fun exists(team: TenantId) = storage.read { BoardTables(it.tables(team)).team.get(Team.SELF) != null }

    fun create(team: TenantId, record: Team) = storage.write { tenants ->
        val teams = BoardTables(tenants.tables(team)).team
        if (teams.get(Team.SELF) != null) false
        else {
            teams.insert(record)
            true
        }
    }
}