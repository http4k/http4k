package exchange.domain

import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import java.util.UUID
import kotlin.math.absoluteValue

class ViewerId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<ViewerId>(::ViewerId)
}

data class Viewer(val id: ViewerId) {
    val name: String
        get() {
            val hash = id.value.hashCode().absoluteValue
            return "${adjectives[hash % adjectives.size]} ${animals[(hash / adjectives.size) % animals.size]}"
        }
}

private val adjectives = listOf("Blue", "Brisk", "Calm", "Clever", "Gentle", "Lucky", "Quiet", "Swift", "Sunny", "Wild")

private val animals = listOf("Otter", "Badger", "Heron", "Lynx", "Panda", "Fox", "Owl", "Hare", "Seal", "Wren")