package org.http4k.format

object KondorJson : ConfigurableKondorJson({
    asConfigurable()
        .withStandardMappings()
        .done()
}) {
    fun custom(configure:AutoMappingConfiguration<Registry>.() -> AutoMappingConfiguration<Registry>) =
        ConfigurableKondorJson({asConfigurable().withStandardMappings().let(configure).done()})
}


