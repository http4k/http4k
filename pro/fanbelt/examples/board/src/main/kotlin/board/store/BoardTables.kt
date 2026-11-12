package board.store

import board.domain.Build
import board.domain.BuildId
import board.domain.Card
import board.domain.CardId
import board.domain.Member
import board.domain.MemberId
import board.domain.Team
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke

class BoardTables(tables: Tables) {
    val cards = tables<CardId, Card>(CARDS)
    val members = tables<MemberId, Member>(MEMBERS)
    val team = tables<String, Team>(TEAMS)
    val builds = tables<BuildId, Build>(BUILDS)

    companion object {
        val CARDS = TableName.of("cards")
        val MEMBERS = TableName.of("members")
        val TEAMS = TableName.of("team")
        val BUILDS = TableName.of("builds")
    }
}