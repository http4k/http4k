package addressbook.oauth.auth

import org.http4k.connect.openai.auth.oauth.PrincipalStore
import org.http4k.connect.storage.Storage
import org.http4k.security.oauth.server.AuthorizationCode

/**
 * Example AuthorizationCode to Principal storage
 */
class StoragePrincipalStore<Principal : Any>(private val codeStorage: Storage<Principal>) : PrincipalStore<Principal> {

    override fun get(key: AuthorizationCode) = codeStorage[key.value].also { codeStorage.remove(key.value) }

    override operator fun set(key: AuthorizationCode, data: Principal) {
        codeStorage[key.value] = data
    }
}
