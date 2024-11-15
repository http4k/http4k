package org.http4k.connect.azure

import de.svenjacobs.loremipsum.LoremIpsum
import org.http4k.connect.azure.action.ModelCompletion
import org.http4k.connect.azure.action.ChatCompletion
import org.http4k.connect.azure.action.Choice
import org.http4k.connect.azure.action.ChoiceDetail
import org.http4k.connect.model.FinishReason
import org.http4k.connect.model.Role
import org.http4k.connect.model.Role.Companion.User
import java.util.Random

/**
 * Helps to control the generation of responses in a particular format for a model.
 */
fun interface ChatCompletionGenerator : (ModelCompletion) -> List<Choice> {
    companion object
}

/**
 * Simply reverses the input question
 */
val ChatCompletionGenerator.Companion.ReverseInput
    get() = ChatCompletionGenerator { req ->
        req.content().flatMap { m ->
            m.content?.mapIndexed { i, content ->
                Choice(i, ChoiceDetail(Role.System, content.text?.reversed() ?: "", null), null, FinishReason.stop)
            } ?: emptyList()
        }
    }

/**
 * Generates Lorem Ipsum paragraphs based on the random generator.
 */
fun ChatCompletionGenerator.Companion.LoremIpsum(random: Random = Random(0)) = ChatCompletionGenerator { req ->
    req.choices(de.svenjacobs.loremipsum.LoremIpsum().getParagraphs(random.nextInt(3, 15)))
}

/**
 * Simply echoes the request
 */
val ChatCompletionGenerator.Companion.Echo
    get() = ChatCompletionGenerator { req ->
        req.choices(req.content().first { it.role == User }.content?.first()?.text ?: "")
    }

private fun ModelCompletion.choices(msg: String) = (if (stream) msg.split(" ").map { "$it " } else listOf(msg))
    .map { Choice(0, ChoiceDetail(Role.System, it, null), null, FinishReason.stop) }
