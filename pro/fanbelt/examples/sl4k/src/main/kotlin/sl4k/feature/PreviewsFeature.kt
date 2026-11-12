package sl4k.feature

import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import sl4k.domain.Unfurled
import sl4k.domain.Unfurler
import sl4k.domain.Unreadable
import sl4k.store.ChatTables
import java.time.Clock
import java.util.Random

class PreviewsFeature(chat: ChatFeature, clock: Clock, random: Random) : Feature<Chat, Unfurler>({ tables -> Chat(ChatTables(tables), clock, random) }) {
    val unfurled by operation { domain, _, found: Unfurled ->
        val preview = domain.unfurled(found) ?: return@operation Changes.none()
        Changes.of(chat.previews.topic(preview.channel))
    }

    val unreadable by operation { domain, _, link: Unreadable ->
        domain.unreadable(link)
        Changes.none()
    }
}
