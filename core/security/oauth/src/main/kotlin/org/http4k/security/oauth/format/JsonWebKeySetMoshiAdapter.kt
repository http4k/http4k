package org.http4k.security.oauth.format

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.http4k.format.obj
import org.http4k.format.string
import org.http4k.format.stringOrNull
import org.http4k.security.oauth.metadata.JsonWebKey
import org.http4k.security.oauth.metadata.JsonWebKeySet

object JsonWebKeySetMoshiAdapter : JsonAdapter<JsonWebKeySet>() {
    @ToJson
    override fun toJson(writer: JsonWriter, value: JsonWebKeySet?) {
        when (value) {
            null -> writer.nullValue()
            else -> with(writer) {
                beginObject()
                name("keys")
                beginArray()
                value.keys.forEach { key ->
                    obj(key) {
                        string("kty", kty)
                        string("use", use)
                        string("kid", kid)
                        string("alg", alg)
                        string("n", n)
                        string("e", e)
                        key.x5c?.let {
                            name("x5c")
                            beginArray()
                            it.forEach { cert -> value(cert) }
                            endArray()
                        }
                        string("x5t", x5t)
                        string("x5t#S256", `x5t#S256`)
                    }
                }
                endArray()
                endObject()
            }
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): JsonWebKeySet {
        val keys = mutableListOf<JsonWebKey>()

        with(reader) {
            beginObject()
            while (hasNext()) {
                when (nextName()) {
                    "keys" -> {
                        beginArray()
                        while (hasNext()) {
                            keys.add(readKey())
                        }
                        endArray()
                    }

                    else -> skipValue()
                }
            }
            endObject()
        }

        return JsonWebKeySet(keys)
    }

    private fun JsonReader.readKey(): JsonWebKey {
        val values = mutableMapOf<String, Any?>()

        beginObject()
        while (hasNext()) {
            when (val name = nextName()) {
                "x5c" -> {
                    val certs = mutableListOf<String>()
                    beginArray()
                    while (hasNext()) {
                        certs.add(nextString())
                    }
                    endArray()
                    values[name] = certs
                }

                else -> values[name] = stringOrNull()
            }
        }
        endObject()

        return with(values) {
            JsonWebKey(
                kty = string("kty") ?: throw JsonDataException("kty was null"),
                use = string("use"),
                kid = string("kid"),
                alg = string("alg"),
                n = string("n"),
                e = string("e"),
                x5c = list("x5c"),
                x5t = string("x5t"),
                `x5t#S256` = string("x5t#S256")
            )
        }
    }

    private fun Map<String, Any?>.string(name: String): String? = this[name] as? String
    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.list(name: String): List<String>? = this[name] as? List<String>
}
