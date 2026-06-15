/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.junit

import org.http4k.storyboard.FrameExtractor
import org.http4k.storyboard.Story.Outcome
import org.http4k.storyboard.Story.Outcome.Aborted
import org.http4k.storyboard.Story.Outcome.Failed
import org.http4k.storyboard.Story.Outcome.Passed
import org.http4k.storyboard.StoryLayout
import org.http4k.storyboard.Storyboard
import org.http4k.storyboard.Theme
import org.http4k.storyboard.defaultExtractors
import org.http4k.storyboard.layout.slideshow.Slideshow
import org.http4k.storyboard.util.StoryboardMoshi
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.opentest4j.TestAbortedException
import java.io.File
import java.time.Clock

class RenderStoryboard(
    private val outputDir: File = File("build/reports/http4k/storyboard"),
    private val theme: Theme = Theme.Http4k,
    private val layout: StoryLayout = Slideshow(theme),
    private val clock: Clock = Clock.systemUTC(),
    private val extractors: List<FrameExtractor> = defaultExtractors
) : BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

    override fun beforeTestExecution(context: ExtensionContext) {
        val sb = Storyboard(
            name = context.requiredTestMethod.name,
            series = context.requiredTestClass.simpleName,
            clock = clock
        )
        store(context).put(Storyboard::class.java, sb)
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val sb = storyboardOf(context) ?: return
        sb.endSession()
        val story = sb.toStory(outcome = context.outcome(), extractors = extractors)

        val testClass = context.requiredTestClass
        val classDir = File(outputDir, testClass.name.replace('.', '/'))
        classDir.mkdirs()

        val safeMethod = context.requiredTestMethod.name.sanitiseForFs()
        File(classDir, "$safeMethod.json").writeText(StoryboardMoshi.asFormatString(story))
        val html = File(classDir, "$safeMethod.html").apply {
            writeText(layout.render(story))
        }

        ClassIndexWriter.write(classDir, testClass.simpleName, theme)
        context.publishReportEntry("storyboard", html.toURI().toString())
    }

    override fun supportsParameter(parameterContext: ParameterContext, context: ExtensionContext): Boolean =
        parameterContext.parameter.type == Storyboard::class.java

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): Storyboard =
        storyboardOf(context)!!

    private fun ExtensionContext.outcome(): Outcome =
        executionException.map { e -> if (e is TestAbortedException) Aborted else Failed }.orElse(Passed) ?: Failed

    private fun storyboardOf(context: ExtensionContext): Storyboard? =
        store(context).get(Storyboard::class.java, Storyboard::class.java)

    private fun store(context: ExtensionContext) =
        context.getStore(Namespace.create(context.requiredTestClass, context.requiredTestMethod))
}

private val unsafeFsChars = Regex("""[/\\:*?"<>|]""")

private fun String.sanitiseForFs(): String =
    replace(unsafeFsChars, "_").trimEnd('.', ' ').ifEmpty { "_" }
