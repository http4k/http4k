package sheets.store

import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.everything
import org.http4k.fanbelt.tenancy.TenantId
import sheets.domain.SheetDetails

class Sheets(private val storage: DocumentStorage) {
    fun all(): List<TenantId> = storage.read { it.having(SheetTables.SHEETS, everything<SheetDetails>()) }.sortedBy { it.value }

    fun exists(sheet: TenantId) = storage.read { SheetTables(it.tables(sheet)).details.get(SheetDetails.SELF) != null }

    fun create(sheet: TenantId, record: SheetDetails) = storage.write { tenants ->
        val records = SheetTables(tenants.tables(sheet)).details
        if (records.get(SheetDetails.SELF) != null) false
        else {
            records.insert(record)
            true
        }
    }
}