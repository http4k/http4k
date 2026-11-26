package features

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import java.time.Instant
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.store.everything
import org.http4k.fanbelt.tenancy.TenantId

class Spaces(private val storage: DocumentStorage) {
    fun all(): List<TenantId> = storage.read { it.having(SpaceTables.SPACES, everything<SpaceDetails>()) }.sortedBy { it.value }

    fun exists(space: TenantId) = storage.read { SpaceTables(it.tables(space)).details.get(SpaceDetails.SELF) != null }

    fun create(space: TenantId, record: SpaceDetails) = storage.write { tenants ->
        val records = SpaceTables(tenants.tables(space)).details
        if (records.get(SpaceDetails.SELF) != null) false
        else {
            records.insert(record)
            true
        }
    }
}

class SpaceTables(tables: Tables) {
    val details = tables<SpaceDetailsId, SpaceDetails>(SPACES)

    companion object {
        val SPACES = TableName.of("space")
    }
}

class SpaceDetailsId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SpaceDetailsId>(::SpaceDetailsId)
}

data class SpaceDetails(val createdAt: Instant, override val id: SpaceDetailsId = SELF) : Identified<SpaceDetailsId> {
    companion object {
        val SELF = SpaceDetailsId.of("self")
    }
}
