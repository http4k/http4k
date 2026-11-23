package boxoffice.web

import boxoffice.domain.SeatRef
import boxoffice.domain.ShowId
import org.http4k.lens.Path
import org.http4k.lens.value

val showId = Path.value(ShowId).of("show")
val seat = Path.value(SeatRef).of("seat")