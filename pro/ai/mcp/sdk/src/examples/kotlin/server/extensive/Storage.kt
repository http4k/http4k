package server.extensive

import org.http4k.config.Environment
import org.http4k.core.HttpHandler
import java.util.Random

interface Storage {
    fun repo(env: Environment, http: HttpHandler, random: Random): FooBarRepo

    companion object {
        object InMemory : Storage {
            override fun repo(env: Environment, http: HttpHandler, random: Random): FooBarRepo {
                TODO("Not yet implemented")
            }
        }
    }
}
