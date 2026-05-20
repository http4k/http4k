package demo

import org.http4k.ai.mcp.util.McpJson.json
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.LocalDate

data class Event(val name: String, val date: LocalDate)

fun EventsServer() = routes(
    "/random-event" bind GET to {
        Response(OK).json(events.random())
    }
)

private val events = listOf(
    Event("KotlinConf 2026", LocalDate.of(2026, 5, 20)),
    Event("10th Anniversary of http4k", LocalDate.of(2027, 5, 17)),
    Event("World's Biggest Kick-About Begins", LocalDate.of(2026, 6, 11)),
    Event("International Lycra Parade Departs Barcelona", LocalDate.of(2026, 7, 4)),
    Event("30th Anniversary of a Photocopied Sheep", LocalDate.of(2026, 7, 5)),
    Event("World's Biggest Kick-About Grand Final", LocalDate.of(2026, 7, 19)),
    Event("57th Anniversary of One Small Step", LocalDate.of(2026, 7, 20)),
    Event("Glasgow Hosts Throwing Heavy Things for Points", LocalDate.of(2026, 7, 23)),
    Event("International Lycra Parade Reaches Paris", LocalDate.of(2026, 7, 26)),
    Event("Global Sun Goes Dark for a Bit", LocalDate.of(2026, 8, 12)),
    Event("534th Anniversary of Getting Hopelessly Lost", LocalDate.of(2026, 10, 12)),
    Event("Southern Hemisphere Tackle Festival", LocalDate.of(2026, 10, 15)),
    Event("509th Anniversary of Nailing Letters to Doors", LocalDate.of(2026, 10, 31)),
    Event("Americans Argue About Who Runs Things", LocalDate.of(2026, 11, 3)),
    Event("Spacecraft Finally Reaches the Closest Planet", LocalDate.of(2026, 11, 1)),
    Event("Mars Gets as Close as It Can Be Bothered", LocalDate.of(2027, 2, 19)),
    Event("Sun Goes Dark Again, Different Continent", LocalDate.of(2027, 8, 2)),
    Event("Women's World Kick-About in Brazil", LocalDate.of(2027, 6, 24)),
    Event("Rugby Goes to Australia", LocalDate.of(2027, 10, 1)),
    Event("Nuclear Helicopter Departs for Saturn's Moon", LocalDate.of(2028, 7, 5)),
    Event("Los Angeles Pretends Traffic Won't Be a Problem", LocalDate.of(2028, 7, 14)),
    Event("Yet Another Sun Goes Dark", LocalDate.of(2028, 7, 22)),
    Event("Los Angeles Admits Traffic Was a Problem", LocalDate.of(2028, 7, 30)),
    Event("100th Anniversary of Pre-Sliced Carbs", LocalDate.of(2028, 7, 7)),
    Event("Americans Argue About Who Runs Things Again", LocalDate.of(2028, 11, 5)),
    Event("Angry Space Rock Buzzes the Planet", LocalDate.of(2029, 4, 13)),
    Event("Competitive Skiing in the French Alps", LocalDate.of(2030, 2, 1)),
    Event("Spacecraft Arrives at Jupiter's Icy Moon", LocalDate.of(2030, 4, 11)),
    Event("World's Biggest Kick-About Returns", LocalDate.of(2030, 6, 8)),
    Event("Riyadh Builds Something Enormous for Visitors", LocalDate.of(2030, 10, 1)),
    Event("Nuclear Helicopter Lands on Saturn's Moon", LocalDate.of(2034, 7, 1)),
    Event("International Dirty Snowball Returns", LocalDate.of(2061, 7, 28)),
    Event("The Heat Death of the Universe", LocalDate.of(9999, 12, 31))
)
