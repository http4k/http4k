package org.http4k.security

import org.http4k.core.cookie.cookie
import org.pac4j.core.context.session.SessionStore
import java.util.*


data class Http4kSession(val id: UUID) {
    val map = mutableMapOf<String, Any>()
    operator fun get(name: String) = map[name]
    operator fun set(name: String, value: Any?) {
        if (value == null) map.remove(name) else map[name] = value
    }
}

class Http4kSessionStore(vararg initSessions: Http4kSession) : SessionStore<Http4kWebContext> {

    private var sessions = mutableMapOf<UUID, Http4kSession>()

    init {
        initSessions.forEach { sessions[it.id] = it }
    }

    private fun getHttp4kSession(context: Http4kWebContext) = context.request.cookie("session")?.let { sessions[UUID.fromString(it.value)] }

    override fun getOrCreateSessionId(context: Http4kWebContext) = getHttp4kSession(context)?.id?.toString() ?: UUID.randomUUID().toString()

    override fun get(context: Http4kWebContext, key: String) = getHttp4kSession(context)?.get(key)

    override fun set(context: Http4kWebContext, key: String, value: Any?) {
        getHttp4kSession(context)?.set(key, value)
    }

    override fun destroySession(context: Http4kWebContext): Boolean {
        getHttp4kSession(context)?.let { sessions.remove(it.id) }
        return true
    }

    override fun getTrackableSession(context: Http4kWebContext) = getHttp4kSession(context)

    override fun buildFromTrackableSession(context: Http4kWebContext, trackableSession: Any): SessionStore<Http4kWebContext> =
        Http4kSessionStore(trackableSession as Http4kSession)

    override fun renewSession(context: Http4kWebContext): Boolean {
        return getHttp4kSession(context)?.let {
            sessions.remove(it.id)
            val new = it.copy(id = UUID.fromString(getOrCreateSessionId(context)))
            sessions.put(new.id, new)
            true
        } ?: false
    }
}
