package org.http4k.storyboard

import freemarker.core.HTMLOutputFormat
import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
import freemarker.template.TemplateExceptionHandler
import org.http4k.format.Moshi
import org.http4k.template.FreemarkerTemplates
import org.http4k.template.ViewModel

private val renderer = FreemarkerTemplates(Configuration(VERSION_2_3_34).apply {
    outputFormat = HTMLOutputFormat.INSTANCE
    templateExceptionHandler = TemplateExceptionHandler.IGNORE_HANDLER
}).CachingClasspath()

fun renderHtml(story: Story): String =
    renderHtml(story, Moshi.asFormatString(story))

fun renderHtml(story: Story, dataJson: String): String =
    renderer(
        StoryboardView(
            testTitle = story.title,
            tiles = story.frames.mapIndexed { i, f -> TileView(i, f.title, i == 0) },
            dataJson = dataJson.replace("</", "<\\/")
        )
    )

internal data class StoryboardView(
    val testTitle: String,
    val tiles: List<TileView>,
    val dataJson: String
) : ViewModel {
    override fun template() = super.template() + ".ftl.html"
}

internal data class TileView(val index: Int, val title: String, val active: Boolean)
