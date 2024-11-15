package org.http4k.connect.anthropic

import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.Content.*
import org.http4k.connect.anthropic.action.Message
import java.util.Random

/**
 * Helps to control the generation of responses in a particular format for a model.
 */
fun interface MessageContentGenerator : (List<Message>) -> List<Text> {
    companion object
}

/**
 * Simply reverses the input question
 */
val MessageContentGenerator.Companion.ReverseInput
    get() = MessageContentGenerator { req ->
        req.last().content.mapIndexed { i, m ->
            when (m) {
                is Text -> Text(m.text.reversed())
                else -> Text("Something about the imager")
            }
        }
    }

/**
 * Generates Lorem Ipsum paragraphs based on the random generator.
 */
fun MessageContentGenerator.Companion.LoremIpsum(random: Random = Random(0)) = MessageContentGenerator { req ->
    List(req.last().content.size) { j ->
        Text(de.svenjacobs.loremipsum.LoremIpsum().getParagraphs(random.nextInt(3, 15)))
    }
}

/**
 * Simply echoes the request
 */
val MessageContentGenerator.Companion.Echo
    get() = MessageContentGenerator { req ->
        req.last().content.map { c ->
            when (c) {
                is Image -> Text("some image content")
                is Text -> Text(c.text)
            }
        }
    }
