package sl4k

import sl4k.domain.ViewerId
import java.util.UUID

fun viewerNamed(name: String) = ViewerId.of(UUID.nameUUIDFromBytes(name.toByteArray()))