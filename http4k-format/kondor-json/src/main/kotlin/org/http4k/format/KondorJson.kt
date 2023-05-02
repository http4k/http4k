package org.http4k.format

object KondorJson : ConfigurableKondorJson({
    asConfigurable()
        .withStandardMappings()
        .done()
}) {
    fun custom(configure:AutoMappingConfiguration<JConverterResolver>.() -> AutoMappingConfiguration<JConverterResolver>) =
        ConfigurableKondorJson({asConfigurable().withStandardMappings().let(configure).done()})
}


