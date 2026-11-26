package features.builds

import dev.forkhandles.values.random
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.feature.zip
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.store.Order.Newest
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.state.topic
import java.util.Random

class BuildLog(tables: Tables, private val random: Random) {
    private val builds = tables<BuildId, Build>(BUILDS)

    fun latest() = builds.page(order = Newest, limit = 5).items

    fun record(status: BuildStatus, commit: Commit) = builds.insert(Build(BuildId.random(random), status, commit))

    companion object {
        val BUILDS = TableName.of("builds")
    }
}

class BuildsFeature(random: Random) : Feature<BuildLog, Ci>({ tables -> BuildLog(tables, random) }) {
    val latest by state { it.latest() }

    val report by operation { log, _, posted: BuildPosted ->
        val (status, commit) = FieldName.of("status").check<BuildStatus>(posted.status, "A build is passing, failing or running")
            .zip(FieldName.of("commit").check(Commit, posted.commit, "A commit is 7 to 40 hex characters")) { status, commit -> status to commit }
            .orElse { return@operation it }
        log.record(status, commit)
        Changes.of(latest.topic())
    }
}
