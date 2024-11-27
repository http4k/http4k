package addressbook.env

import org.http4k.chaos.defaultPort
import org.http4k.chaos.start
import org.http4k.connect.openai.FakeOpenAI

/**
 * This program runs several plugins and Fake OpenAI server. Run it and browse to the local port
 * to demonstrate the login flows and then click through to exercise the plugin APIs.
 */
fun main() {
    val openAiPort = FakeOpenAI::class.defaultPort

    FakeOpenAI(
        plugins = arrayOf(
            startNoAuthPlugin(),
            startOAuthPlugin(openAiPort),
            startServicePlugin(),
            startUserPlugin()
        )
    ).start()

    println("Login to http://localhost:${openAiPort}")
}

