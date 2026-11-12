package helpdesk.store

import helpdesk.domain.Org
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.everything
import org.http4k.fanbelt.tenancy.TenantId

class Orgs(private val storage: DocumentStorage) {
    fun all(): List<TenantId> = storage.read { it.having(HelpdeskTables.ORGS, everything<Org>()) }.sortedBy { it.value }

    fun exists(org: TenantId) = storage.read { HelpdeskTables(it.tables(org)).org.get(Org.SELF) != null }

    fun create(org: TenantId, record: Org) = storage.write { tenants ->
        val orgs = HelpdeskTables(tenants.tables(org)).org
        if (orgs.get(Org.SELF) != null) false
        else {
            orgs.insert(record)
            true
        }
    }
}