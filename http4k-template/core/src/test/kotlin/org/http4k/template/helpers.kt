package org.http4k.template

object NonExistent : ViewModel {
    override fun template() = "bibble"
}

data class OnClasspath(val items: List<Item>) : ViewModel {
    override fun template() = javaClass.name.replace('.', '/')
}

data class OnClasspathNotAtRoot(val items: List<Item>) : ViewModel {
    override fun template() = javaClass.simpleName
}

data class AtRoot(val items: List<Item>) : ViewModel {
    override fun template() = "AtRootBob"
}

data class Feature(val description: String)

data class Item(val name: String, val price: String, val features: List<Feature>)

object TemplateA : ViewModel
object TemplateB : ViewModel
object TemplateC : ViewModel
