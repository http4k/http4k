package helpdesk.feature

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import helpdesk.domain.ViewerId
import org.http4k.connect.openfga.OpenFga
import org.http4k.connect.openfga.batchCheck
import org.http4k.connect.openfga.check
import org.http4k.connect.openfga.createStore
import org.http4k.connect.openfga.model.BatchCheckItem
import org.http4k.connect.openfga.model.CorrelationId
import org.http4k.connect.openfga.model.FgaObject
import org.http4k.connect.openfga.model.OnConflict.IGNORE
import org.http4k.connect.openfga.model.Relation
import org.http4k.connect.openfga.model.RelationDefinition.Companion.direct
import org.http4k.connect.openfga.model.RelationReference
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.StoreName
import org.http4k.connect.openfga.model.TupleKey
import org.http4k.connect.openfga.model.TypeDefinition
import org.http4k.connect.openfga.model.TypeName
import org.http4k.connect.openfga.model.User
import org.http4k.connect.openfga.write
import org.http4k.connect.openfga.writeAuthorizationModel
import org.http4k.connect.orThrow
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Clock
import java.time.Duration
import java.time.Instant

class OrgPolicy(private val openFga: OpenFga, private val store: StoreId, private val clock: Clock, val recheckEvery: Duration = Duration.ofSeconds(30)) {
    fun recheckAt(): Instant = clock.instant() + recheckEvery

    fun isAgent(org: TenantId, agent: ViewerId) =
        openFga.check(store, agentOf(org, agent.user())).map { it.allowed }.valueOrNull() == true

    fun agentsAmong(org: TenantId, candidates: Set<ViewerId>) = candidates.chunked(MAX_CHECKS_PER_BATCH).flatMap { chunk ->
        val allowed = openFga.batchCheck(store, chunk.map { BatchCheckItem(agentOf(org, it.user()), CorrelationId.of(it.toString())) }).orThrow().result
        chunk.filter { allowed[CorrelationId.of(it.toString())]?.allowed == true }
    }.toSet()

    fun appoint(org: TenantId, agent: ViewerId) = openFga.write(store, writes = listOf(agentOf(org, agent.user()))).orThrow()

    fun appointEveryone(org: TenantId) = openFga.write(store, writes = listOf(agentOf(org, User.of("user:*"))), onDuplicate = IGNORE).orThrow()

    fun dismiss(org: TenantId, agent: ViewerId) = openFga.write(store, deletes = listOf(agentOf(org, agent.user()))).orThrow()

    companion object {
        fun create(openFga: OpenFga, clock: Clock): OrgPolicy {
            val store = openFga.createStore(StoreName.of("helpdesk")).orThrow().id
            openFga.writeAuthorizationModel(
                store,
                listOf(TypeDefinition.of(user), TypeDefinition.of(org, agent to direct(RelationReference(user), RelationReference.wildcard(user))))
            ).orThrow()
            return OrgPolicy(openFga, store, clock)
        }
    }
}

private val user = TypeName.of("user")
private val org = TypeName.of("org")
private val agent = Relation.of("agent")

private const val MAX_CHECKS_PER_BATCH = 50

private fun TenantId.fga() = FgaObject.of("org:$this")

private fun ViewerId.user() = User.of("user:$this")

private fun agentOf(org: TenantId, who: User) = TupleKey(who, agent, org.fga())
