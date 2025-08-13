package org.http4k.template

import htmlflow.HtmlFlow
import htmlflow.HtmlView
import htmlflow.dyn
import htmlflow.html
import org.xmlet.htmlapifaster.body
import org.xmlet.htmlapifaster.li
import org.xmlet.htmlapifaster.span
import org.xmlet.htmlapifaster.ul

val onClassPath: HtmlView<OnClasspath> =
    HtmlFlow.view { view ->
        view
            .html {
                body {
                    ul {
                        dyn { model: OnClasspath ->
                            model.items.forEach { item ->
                                li {
                                    text("Name:")
                                    span { text(item.name) }
                                    text("Price:")
                                    span { text(item.price) }
                                    ul {
                                        item.features.forEach { feature ->
                                            li {
                                                text("Feature:")
                                                span { text(feature.description) }
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


