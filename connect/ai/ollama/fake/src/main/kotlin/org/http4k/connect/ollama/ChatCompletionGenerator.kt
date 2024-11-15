package org.http4k.connect.ollama

import java.util.Random

fun interface ChatCompletionGenerator : (List<Message>, ResponseFormat?) -> List<String> {
    companion object
}

fun ChatCompletionGenerator.Companion.LoremIpsum(random: Random = Random(0)) = ChatCompletionGenerator { _, _ ->
    listOf(de.svenjacobs.loremipsum.LoremIpsum().getParagraphs(random.nextInt(3, 15)))
}

