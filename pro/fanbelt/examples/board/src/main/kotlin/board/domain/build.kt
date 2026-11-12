package board.domain

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.regex
import org.http4k.fanbelt.store.Identified
import java.util.UUID

class BuildId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<BuildId>(::BuildId)
}

class Commit private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<Commit>(::Commit, "[0-9a-f]{7,40}".regex)
}

enum class BuildStatus { Passing, Failing, Running }

data class Build(override val id: BuildId, val board: BoardId, val status: BuildStatus, val commit: Commit) : Identified<BuildId>

data class BuildPosted(val status: String, val commit: String)

data class BuildReport(val board: BoardId, val status: BuildStatus, val commit: Commit)

data object Ci