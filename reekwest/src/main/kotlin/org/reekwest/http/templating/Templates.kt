package org.reekwest.http.templating

/**
 * Supported template implementations for templating engine implementations
 */
interface Templates {

    /**
     * Loads and caches templates from the compiled classpath
     *
     * @param baseClasspathPackage the root package to load from (funaults to
     */
    fun CachingClasspath(baseClasspathPackage: String = ""): TemplateRenderer

    /**
     * Load and caches templates from a file path
     *
     * @param baseTemplateDir the root path to load templates from
     */
    fun Caching(baseTemplateDir: String = "./"): TemplateRenderer

    /**
     * Hot-reloads (no-caching) templates from a file path
     *
     * @param baseTemplateDir the root path to load templates from
     */
    fun HotReload(baseTemplateDir: String = "./"): TemplateRenderer
}

