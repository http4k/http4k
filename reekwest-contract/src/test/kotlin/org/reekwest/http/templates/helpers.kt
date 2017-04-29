package org.reekwest.http.templates

object NonExistent : View {
    override fun template() = "bibble"
}

data class OnClasspath(val items: List<Item>) : View {
    override fun template() = javaClass.name.replace('.', '/')
}

data class AtRoot(val items: List<Item>) : View {
    override fun template() = "AtRootBob"
}

data class Feature(val description: String)

data class Item(val name: String, val price: String, val features: List<Feature>)
