package addressbook.oauth.auth

import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage

/**
 * Provides instances of in-memory storage for the given type of object.
 */
class InMemoryStorageProvider {
    operator fun <T : Any> invoke() = Storage.InMemory<T>()
}
