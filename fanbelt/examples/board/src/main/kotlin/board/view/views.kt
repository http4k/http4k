package board.view

import board.domain.BoardId
import board.domain.CardId
import board.domain.CardText
import board.domain.Role
import board.domain.ViewerId
import org.http4k.core.Uri
import org.http4k.fanbelt.page.PageShell
import org.http4k.template.ViewModel

data class CardSignals(val text: String = "")

data class CardView(val id: CardId, val text: CardText, val canLeft: Boolean, val canRight: Boolean)

data class ColumnView(val name: String, val cards: List<CardView>)

data class CardsView(val board: BoardId, val editor: Boolean, val columns: List<ColumnView>) : ViewModel

data class ViewerView(
    val id: ViewerId,
    val name: String,
    val role: Role,
    val you: Boolean,
    val canPromote: Boolean,
    val canDemote: Boolean,
)

data class ViewersView(val board: BoardId, val viewers: List<ViewerView>) : ViewModel

data class BoardPageView(val shell: PageShell, val board: BoardId, val you: String) : ViewModel

data class TeamLink(val name: String, val href: Uri)

data class TeamsPageView(val teams: List<TeamLink>, val typed: String, val error: String?) : ViewModel

data class BuildRow(val status: String, val commit: String)

data class BuildsView(val status: String, val builds: List<BuildRow>) : ViewModel