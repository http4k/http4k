package board

import board.domain.ViewerId
import java.util.UUID

fun viewerNamed(name: String) = ViewerId.of(UUID.nameUUIDFromBytes(name.toByteArray()))