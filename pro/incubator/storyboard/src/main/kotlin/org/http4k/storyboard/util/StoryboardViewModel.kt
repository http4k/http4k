package org.http4k.storyboard.util

import org.http4k.template.ViewModel

interface StoryboardViewModel : ViewModel {
    override fun template() = super.template() + ".ftl.html"
}
