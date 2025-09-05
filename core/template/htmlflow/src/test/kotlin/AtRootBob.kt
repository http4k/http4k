import htmlflow.HtmlFlow
import htmlflow.HtmlView
import htmlflow.dyn
import htmlflow.html
import org.http4k.template.AtRoot
import org.xmlet.htmlapifaster.body
import org.xmlet.htmlapifaster.li
import org.xmlet.htmlapifaster.span
import org.xmlet.htmlapifaster.ul

val atRootBob: HtmlView<AtRoot> =
    HtmlFlow.view<AtRoot> { page ->
        page.html {
            body {
                dyn { model: AtRoot ->
                    ul {
                        model.items.forEach { item ->
                            li {
                                text("AtRootName:")
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
