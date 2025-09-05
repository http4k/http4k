package org.http4k.template

import htmlflow.HtmlFlow
import htmlflow.HtmlView
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.jvm.java

/**
 * HtmlFlow Templates implementation for rendering HTML views using the
 * [HtmlFlow](https://github.com/xmlet/HtmlFlow) engine.
 *
 * This implementation scans the classpath for classes containing [HtmlView] fields/methods and
 * automatically registers them for template rendering. It supports various class patterns
 * including:
 *
 * ### Kotlin Objects
 *
 * ```kotlin
 * object MyViews {
 *     val userView: HtmlView<UserViewModel> = HtmlFlow.view { ... }
 * }
 * ```
 *
 * ### Class with Default Constructor
 *
 * ```kotlin
 * class ViewRepository {
 *     val profileView: HtmlView<ProfileViewModel> = HtmlFlow.view { ... }
 * }
 * ```
 *
 * ### Factory Methods
 *
 * ```kotlin
 * class ViewFactory {
 *     fun getUserDashboard(): HtmlView<DashboardViewModel> = HtmlFlow.view { ... }
 * }
 * ```
 *
 * ### Top-Level Kotlin Properties
 *
 * ```kotlin
 * val topLevelView: HtmlView<TopLevelViewModel> = HtmlFlow.view { ... }
 * ```
 *
 * ### Top-Level Kotlin Functions
 *
 * ```kotlin
 * fun topLevelView(): HtmlView<TopLevelViewModel> {
 *    return HtmlFlow.view { ... }
 * }
 * ```
 * If the views are explicitly configured, [HtmlView] the pre encoding configuration will be overridden based
 * on the method used to create the [TemplateRenderer]. [Caching] uses pre-encoding of static HTML
 * blocks, while [HotReload] evaluates the template on each render.
 *
 * [HtmlViewAsync] and [HtmlViewSuspend] are not supported in this implementation. Additionally,
 * Since HtmlFlow is an [internal DSL](https://martinfowler.com/bliki/InternalDslStyle.html), It
 * does not support directory scanning for templates, so you must use the classpath scanning
 * methods.
 *
 */
class HtmlFlowTemplates : Templates {

    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer {
        val viewRegistry = scanForHtmlViews(baseClasspathPackage, false)
        return createTemplateRenderer(viewRegistry, false)
    }

    fun HotReloadClasspath(baseClasspathPackage: String = ""): TemplateRenderer {
        val viewRegistry = scanForHtmlViews(baseClasspathPackage, true)
        return createTemplateRenderer(viewRegistry, true)
    }

    override fun Caching(baseTemplateDir: String): TemplateRenderer {
        val packagePath = baseTemplateDir
            .replace(Regex("^(src[/\\\\](main|test)[/\\\\]kotlin([/\\\\])?)"), "")
            .replace(Regex("[/\\\\]"), ".")
            .trim('.')
        val viewRegistry = scanForHtmlViews(packagePath, false)
        return createTemplateRenderer(viewRegistry, false)
    }

    override fun HotReload(baseTemplateDir: String): TemplateRenderer {
        val packagePath = baseTemplateDir
            .replace(Regex("^(src[/\\\\](main|test)[/\\\\]kotlin([/\\\\])?)"), "")
            .replace(Regex("[/\\\\]"), ".")
            .trim('.')
        val viewRegistry = scanForHtmlViews(packagePath, false)
        return createTemplateRenderer(viewRegistry, true)
    }

    companion object {
        /**
         * Thread-safe cache for preprocessed view registries, keyed by scanned package name. This
         * prevents redundant classpath scanning for the same packages in caching mode.
         */
        private val cachedViewRegistries = ConcurrentHashMap<String, Map<Class<*>, ViewInfo>>()

        /**
         * Cache for hot reload view registries, keyed by scanned package name. This is cleared on
         * each hot reload to ensure fresh view discovery.
         */
        private val hotViewRegistries = ConcurrentHashMap<String, Map<Class<*>, ViewInfo>>()

        /**
         * Thread-safe cache for preprocessed view resolution lookups to optimize repeated access to
         * derived types.
         *
         * When a ViewModel class is resolved via inheritance chain, interfaces, or assignable types
         * (rather than direct registry lookup), the result is cached here to avoid re-traversing
         * the resolution chain on subsequent renders of the same concrete type.
         *
         * This cache is separate from the main registry to preserve semantic clarity:
         * - Registry contains only explicitly declared generic types (e.g., HtmlView<BaseVm>)
         * - Resolution cache contains inferred mappings (e.g., DerivedVm -> BaseVm's view)
         */
        private val resolutionCache = ConcurrentHashMap<Class<*>, ViewInfo>()

        /**
         * Cache for hot reload view resolution lookups. This is cleared on each hot reload to
         * ensure fresh view resolution.
         */
        private val hotResolutionCache = ConcurrentHashMap<Class<*>, ViewInfo>()

        /**
         * Represents metadata about a discovered HTML view during classpath scanning.
         *
         * This data class encapsulates all the information needed to identify, locate, and use a
         * discovered view for template rendering.
         *
         * @property view The actual view instance that can render
         *   templates
         * @property location Human-readable location description for debugging and error messages
         *   (e.g., "com.example.Views.userView" or "com.example.ViewFactory.createUserView()")
         * @property viewModelType The type of ViewModel this view expects as input for rendering
         * @see HtmlView
         */
        class ViewInfo(
            val view: Any,
            val location: String,
            val viewModelType: Class<*>,
        )

        /**
         * Packages to skip during classpath scanning for performance optimization. These are known
         * system/library packages that won't contain HTML views.
         */
        private val EXCLUDED_PACKAGES =
            setOf(
                // Kotlin stdlib
                "kotlin",
                "kotlinx",
                // Java stdlib
                "java",
                "javax",
                "jdk",
                "sun",
                "com.sun",
                "com.oracle",
                // Common libraries that won't have HTML views
                "org.slf4j",
                "ch.qos.logback",
                "org.apache.logging",
                "org.junit",
                "org.mockito",
                "org.springframework",
                "org.jetbrains.annotations",
            )
    }

    /**
     * Scans the specified package for HTML views, using caching to avoid redundant scans.
     *
     * @param basePackage The root package to scan (empty string scans all packages). Use dot
     *   notation like "com.example.views"
     *
     * @return Registry mapping ViewModel types to their corresponding view information. Each
     *   ViewInfo contains the view instance, location metadata, and type information.
     * @throws IllegalStateException if duplicate views are found for the same ViewModel type
     * @throws ClassNotFoundException if referenced classes cannot be loaded
     * @throws IllegalAccessException if view fields/methods cannot be accessed
     */
    private fun scanForHtmlViews(basePackage: String = "", hot: Boolean): Map<Class<*>, ViewInfo> {
        val cacheKey = "$basePackage:${hot}"
        val targetRegistry = if (hot) hotViewRegistries else cachedViewRegistries
        return targetRegistry.computeIfAbsent(cacheKey) { _ ->
            performPackageScan(basePackage, hot)
        }
    }

    /**
     * Performs the classpath scanning for the given package.
     *
     * Iterates through all available classloaders attempting to enumerate resources that match the
     * provided base package. For each discovered root it delegates to specific resource processors
     * (filesystem or jar).
     * - The last classloader rethrows the exception to surface hard failures.
     *
     * @param basePackage Package to scan. Empty string means full classpath
     *   (discouraged for perf).
     * @return Immutable map of discovered view model class -> ViewInfo.
     */
    private fun performPackageScan(basePackage: String, hot: Boolean): Map<Class<*>, ViewInfo> {
        val viewRegistry = mutableMapOf<Class<*>, ViewInfo>()
        val classLoaders = getAvailableClassLoaders()
        val packagePath = basePackage.replace('.', '/')

        var resourcesFound = false

        for ((index, classLoader) in classLoaders.withIndex()) {
            try {
                val resources = classLoader.getResources(packagePath)
                while (resources.hasMoreElements()) {
                    resourcesFound = true
                    val resource = resources.nextElement()
                    processClasspathResource(resource, basePackage, viewRegistry, hot)
                }
                if (resourcesFound) break
            } catch (e: Exception) {
                if (index == classLoaders.lastIndex) {
                    throw e
                }
            }
        }

        return viewRegistry.toMap()
    }

    /**
     * Provides an ordered and de-duplicated list of candidate classloaders used for discovery.
     *
     * Order of precedence:
     * 1. Thread context classloader
     * 2. The library's own defining classloader
     * 3. The JVM system classloader
     *
     * Duplicates (e.g. when context == system) are removed while preserving order.
     */
    private fun getAvailableClassLoaders(): List<ClassLoader> {
        return listOfNotNull(
            Thread.currentThread().contextClassLoader,
            this::class.java.classLoader,
            ClassLoader.getSystemClassLoader(),
        )
            .distinct()
    }

    /**
     * Dispatches handling of a concrete classpath resource based on its URL protocol.
     *
     * Supported protocols:
     * - file : walks the directory tree rooted at the package path
     * - jar : streams jar entries filtering for classes under the package path
     */
    private fun processClasspathResource(
        resource: URL,
        basePackage: String,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean,
    ) {
        when (resource.protocol) {
            "file" -> processFileSystemResource(resource, basePackage, viewRegistry, hot)
            "jar" -> processJarResource(resource, basePackage, viewRegistry, hot)
        }
    }

    /**
     * Walks a filesystem directory that corresponds to the target package and recursively delegates
     * into sub-packages, loading candidate class files for further inspection.
     */
    private fun processFileSystemResource(
        resource: URL,
        basePackage: String,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean,
    ) {
        val packageDirectory = File(resource.toURI())
        if (packageDirectory.exists() && packageDirectory.isDirectory) {
            scanDirectory(packageDirectory, basePackage, viewRegistry, hot)
        }
    }

    /**
     * Streams entries of a JAR file and loads only class files that belong to (or are nested under)
     * the target package. Excluded packages are skipped early to reduce I/O and class loading.
     */
    private fun processJarResource(
        resource: URL,
        basePackage: String,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean,
    ) {
        val jarPath = resource.path.substringBefore("!")
        val jarFile = JarFile(URI(jarPath).path)
        val packagePath = basePackage.replace('.', '/')

        jarFile
            .entries()
            .asSequence()
            .filter { entry -> isRelevantClassEntry(entry, packagePath) }
            .forEach { entry ->
                val className = convertEntryNameToClassName(entry.name)
                if (!shouldExcludeClass(className)) {
                    loadAndScanClass(className, viewRegistry, hot)
                }
            }
    }

    /**
     * Recursively scans a directory structure resolving fully qualified class names from relative
     * paths, applying exclusion rules and delegating each discovered class to the reflective
     * loading pipeline.
     */
    private fun scanDirectory(
        directory: File,
        packageName: String,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean,
    ) {
        if (shouldExcludePackage(packageName)) {
            return
        }

        val files = directory.listFiles() ?: return

        files.forEach { file ->
            when {
                file.isDirectory -> {
                    val subPackage = buildSubPackageName(packageName, file.name)
                    scanDirectory(file, subPackage, viewRegistry, hot)
                }

                isRelevantClassFile(file) -> {
                    val className = buildClassName(packageName, file.nameWithoutExtension)
                    loadAndScanClass(className, viewRegistry, hot)
                }
            }
        }
    }

    /**
     * Attempts to load a class by name and, if successful, inspects it (and its associated Kotlin
     * file facade where applicable) for view definitions.
     *
     * Exclusion checks are performed before any reflective work. Non-fatal linkage / definition
     * issues for individual classes are skipped; unexpected exceptions propagate so
     * callers can decide fail strategy.
     */
    private fun loadAndScanClass(
        className: String,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean
    ) {
        if (shouldExcludeClass(className)) {
            return
        }

        try {
            val clazz = Class.forName(className)
            scanClassForHtmlViews(clazz, viewRegistry, hot)

            if (!className.endsWith("Kt")) {
                tryLoadAndScanKotlinFileClass(className, viewRegistry, hot)
            }
        } catch (_: ClassNotFoundException) {
        } catch (_: NoClassDefFoundError) {
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Attempts to load and scan the Kotlin file (Kt) class in order to process top-level properties
     * or functions returning views.
     */
    private fun tryLoadAndScanKotlinFileClass(
        originalClassName: String,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean
    ) {
        try {
            val kotlinFileClassName = "${originalClassName}Kt"
            val kotlinFileClass = Class.forName(kotlinFileClassName)
            scanClassForHtmlViews(kotlinFileClass, viewRegistry, hot)
        } catch (_: Exception) {
            // no-op
        }
    }

    private fun scanClassForHtmlViews(
        clazz: Class<*>,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean
    ) {
        if (shouldSkipClass(clazz)) return
        try {
            val objectInstance = createInstanceForScanning(clazz)
            if (objectInstance == null && !clazz.constructors.isEmpty()) {
                return
            }
            scanClassMembers(clazz, objectInstance, viewRegistry, hot)
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Creates an instance of a class for scanning purposes using multiple instantiation strategies.
     *
     * For Kotlin file classes (ending with "Kt"), returns null since top-level properties don't
     * require instance creation.
     *
     * @param clazz The class to instantiate for view scanning
     * @return Class instance for scanning, or null if no suitable instantiation method found or if
     *   the class represents Kotlin top-level declarations
     */
    private fun createInstanceForScanning(
        clazz: Class<*>,
        factory: HtmlFlow.ViewFactory? = null
    ): Any? {
        // For Kotlin file classes (top-level properties), we don't need an instance
        if (clazz.name.endsWith("Kt")) {
            return null
        }

        return tryObjectInstance(clazz)
            ?: tryDefaultConstructor(clazz)
            ?: tryViewFactoryConstructor(clazz, factory)
            ?: tryCompanionObject(clazz)
            ?: trySingletonInstance(clazz)
    }

    private fun tryObjectInstance(clazz: Class<*>): Any? {
        return try {
            clazz.getDeclaredField("INSTANCE").get(null)
        } catch (_: Exception) {
            null
        }
    }

    private fun tryCompanionObject(clazz: Class<*>): Any? {
        return try {
            val companionClass = clazz.declaredClasses.find { it.simpleName == "Companion" }
            companionClass?.getDeclaredField("INSTANCE")?.get(null)
        } catch (_: Exception) {
            null
        }
    }

    private fun trySingletonInstance(clazz: Class<*>): Any? {
        return try {
            clazz.getDeclaredMethod("getInstance").invoke(null)
        } catch (_: Exception) {
            null
        }
    }

    private fun tryDefaultConstructor(clazz: Class<*>): Any? {
        return try {
            clazz.getDeclaredConstructor().newInstance()
        } catch (_: Exception) {
            null
        }
    }

    private fun tryViewFactoryConstructor(clazz: Class<*>, factory: HtmlFlow.ViewFactory?): Any? {
        if (factory == null) return null

        return try {
            val constructor = clazz.getDeclaredConstructor(HtmlFlow.ViewFactory::class.java)
            constructor.isAccessible = true
            constructor.newInstance(factory)
        } catch (_: NoSuchMethodException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Performs scanning of both methods and fields of the supplied class instance for HtmlView
     * definitions.
     */
    private fun scanClassMembers(
        clazz: Class<*>,
        objectInstance: Any?,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean
    ) {
        scanMethodsForViews(clazz, objectInstance, viewRegistry, hot)
        scanFieldsForViews(clazz, objectInstance, viewRegistry, hot)
    }

    /**
     * Identifies zero-arg methods whose return type is assignable to HtmlView and invokes them
     * (lazily instantiating the containing class if required) registering each resulting view.
     */
    private fun scanMethodsForViews(
        clazz: Class<*>,
        objectInstance: Any?,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean,
    ) {
        clazz.declaredMethods
            .filter { method -> isRelevantViewMethod(method) }
            .forEach { method -> processViewMethod(method, objectInstance, viewRegistry, hot) }
    }

    /**
     * Inspects declared fields for HtmlView types, accessing private/protected members when needed
     * (by setting accessibility) and registering discovered view instances.
     */
    private fun scanFieldsForViews(
        clazz: Class<*>,
        objectInstance: Any?,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean,
    ) {
        clazz.declaredFields
            .filter { field -> isRelevantViewField(field) }
            .forEach { field -> processViewField(field, objectInstance, viewRegistry, hot) }
    }

    /**
     * Fast predicate used while streaming jar entries to narrow down to concrete top-level class
     * files under the target package path.
     */
    private fun isRelevantClassEntry(
        entry: JarEntry,
        packagePath: String,
    ): Boolean {
        return !entry.isDirectory &&
            entry.name.startsWith(packagePath) &&
            entry.name.endsWith(".class")
    }

    /**
     * Converts a jar entry path (e.g. com/example/Foo.class) into a canonical FQN (e.g.
     * com.example.Foo).
     */
    private fun convertEntryNameToClassName(entryName: String): String {
        return entryName.removeSuffix(".class").replace('/', '.')
    }

    /**
     * Determines whether a package should be skipped based on static exclusion prefixes (used to
     * avoid descending into large framework / JDK namespaces).
     */
    private fun shouldExcludePackage(packageName: String): Boolean {
        return EXCLUDED_PACKAGES.any { excludedPackage -> packageName.startsWith(excludedPackage) }
    }

    /**
     * Determines whether a fully qualified class name should be ignored according to the same
     * exclusion rules as packages (simple prefix comparison).
     */
    private fun shouldExcludeClass(className: String): Boolean {
        return EXCLUDED_PACKAGES.any { excludedPackage -> className.startsWith(excludedPackage) }
    }

    /**
     * Invokes a candidate method and registers the resulting HtmlView.
     */
    private fun processViewMethod(
        method: Method,
        objectInstance: Any?,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean,
    ) {
        method.isAccessible = true
        val originalView =
            if (
                method.parameterCount == 1 &&
                method.parameterTypes[0] == HtmlFlow.ViewFactory::class.java
            ) {
                method.invoke(objectInstance)
            } else {
                method.invoke(objectInstance)
            } ?: return
        val location = "${method.declaringClass.name}.${method.name}()"

        val viewToRegister = (originalView as? HtmlView<*>)?.setPreEncoding(!hot) ?: return

        registerView(viewToRegister, method.genericReturnType, viewRegistry, location)
    }

    /**
     * Reads a candidate HtmlView field and registers it
     */
    private fun processViewField(
        field: Field,
        objectInstance: Any?,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        hot: Boolean,
    ) {
        try {
            field.isAccessible = true
            val originalView = field.get(objectInstance) ?: return
            val location = "${field.declaringClass.name}.${field.name}"
            val viewToRegister = (originalView as? HtmlView<*>)?.setPreEncoding(!hot) ?: return

            registerView(viewToRegister, field.genericType, viewRegistry, location)
        } catch (_: Exception) {
            // No-op
        }
    }

    /**
     * Determines whether the file corresponds to a top-level compiled class (filters out synthetic
     * / inner classes by excluding names containing the '$' delimiter).
     */
    private fun isRelevantClassFile(file: File): Boolean {
        return file.name.endsWith(".class") && !file.name.contains('$')
    }

    /**
     * Constructs the new package FQN when descending into a subdirectory during filesystem scan.
     */
    private fun buildSubPackageName(
        parentPackage: String,
        directoryName: String,
    ): String {
        return if (parentPackage.isEmpty()) directoryName else "$parentPackage.$directoryName"
    }

    /** Joins the package name and simple class name into a FQN, handling root package edge case. */
    private fun buildClassName(
        packageName: String,
        simpleClassName: String,
    ): String {
        return if (packageName.isEmpty()) simpleClassName else "$packageName.$simpleClassName"
    }

    /** Early exclusion predicate for types that cannot hold concrete HtmlView instances. */
    private fun shouldSkipClass(clazz: Class<*>): Boolean {
        return clazz.isInterface || clazz.isAnnotation || clazz.isEnum
    }

    /**
     * Predicate identifying candidate methods (zero args or single Engine arg, non-synthetic,
     * HtmlView return type).
     */
    private fun isRelevantViewMethod(method: Method): Boolean {
        return !method.isSynthetic &&
            (method.parameterCount == 0 ||
                (method.parameterCount == 1 &&
                    method.parameterTypes[0] == HtmlFlow.ViewFactory::class.java)) &&
            isHtmlViewMethod(method)
    }

    private fun isHtmlViewMethod(method: Method): Boolean =
        HtmlView::class.java.isAssignableFrom(method.returnType)

    private fun isRelevantViewField(field: Field): Boolean {
        return !field.isSynthetic && isHtmlViewField(field)
    }

    private fun isHtmlViewField(field: Field): Boolean =
        HtmlView::class.java.isAssignableFrom(field.type)

    /**
     * Registers a discovered view in the registry.
     *
     * @throws IllegalStateException if multiple views are found for the same ViewModel type
     */
    private fun registerView(
        view: Any,
        genericType: Type,
        viewRegistry: MutableMap<Class<*>, ViewInfo>,
        viewLocation: String,
    ) {
        val viewModelType = extractViewModelType(genericType) ?: return

        checkForDuplicateViewRegistration(viewRegistry, viewModelType, viewLocation)

        viewRegistry[viewModelType] = ViewInfo(view, viewLocation, viewModelType)
    }

    /** Checks for duplicate view registrations and throws an exception if found. */
    private fun checkForDuplicateViewRegistration(
        viewRegistry: Map<Class<*>, ViewInfo>,
        viewModelType: Class<*>,
        newLocation: String,
    ) {
        val existing = viewRegistry[viewModelType]
        if (existing != null) {
            throw IllegalStateException(
                "Multiple views found for ViewModel type '${viewModelType.simpleName}'. " +
                    "Existing: ${existing.location}, New: $newLocation",
            )
        }
    }

    private fun extractViewModelType(genericType: Type): Class<*>? {
        return when (genericType) {
            is ParameterizedType -> extractFromParameterizedType(genericType)
            is Class<*> -> extractFromClassType(genericType)
            else -> null
        }
    }

    private fun extractFromParameterizedType(parameterizedType: ParameterizedType): Class<*>? {
        val typeArguments = parameterizedType.actualTypeArguments
        return if (typeArguments.isNotEmpty()) {
            typeArguments[0] as? Class<*>
        } else {
            null
        }
    }

    private fun extractFromClassType(classType: Class<*>): Class<*>? {
        val superType = classType.genericSuperclass
        return if (superType is ParameterizedType) {
            extractViewModelType(superType)
        } else {
            null
        }
    }

    /** Creates a template renderer from the view registry. */
    private fun createTemplateRenderer(
        viewRegistry: Map<Class<*>, ViewInfo>,
        hot: Boolean
    ): TemplateRenderer {
        val targetResolutionCache = if (hot) hotResolutionCache else resolutionCache
        return object : TemplateRenderer {
            override fun invoke(viewModel: ViewModel): String {
                val viewInfo = findCompatibleView(viewModel, viewRegistry, targetResolutionCache)
                return renderViewWithModel(viewInfo.view, viewModel, viewInfo.location)
            }
        }
    }

    /**
     * Finds a compatible view for the given ViewModel by applying a precedence chain with caching:
     * 1. Exact class match in registry (fastest)
     * 2. Resolution cache lookup (for previously resolved indirect matches)
     * 3. Full resolution chain: a. Superclass match (nearest ancestor first) b. Direct interface
     *    match c. Assignable match (any registered type that isAssignableFrom the model's class)
     * 4. Cache successful resolution for future lookups
     *
     * Example usages:
     * ```kotlin
     * // 1. Direct match
     * data class UserVm(val name: String) : ViewModel
     * val userView: HtmlView<UserVm> = HtmlFlow.view { ... }
     * // registry contains (UserVm -> userView). Rendering UserVm finds userView immediately.
     *
     * // 2. Cached resolution (second+ render of derived type)
     * open class BaseVm : ViewModel
     * class DerivedVm : BaseVm()
     * // First render: walks inheritance chain, finds BaseVm view, caches DerivedVm -> BaseVm view
     * // Second render: cache hit, no traversal needed
     *
     * // 3. Superclass match
     * open class BaseVm : ViewModel
     * class DerivedVm : BaseVm()
     * val baseView: HtmlView<BaseVm> = HtmlFlow.view { ... }
     * // registry has BaseVm only; rendering DerivedVm walks inheritance and uses baseView.
     *
     * // 4. Interface match
     * interface ProfileLike : ViewModel { val name: String }
     * data class PublicProfile(override val name: String) : ProfileLike
     * val profileView: HtmlView<ProfileLike> = HtmlFlow.view { ... }
     * // registry key is ProfileLike; rendering PublicProfile matches interface.
     * ```
     */
    private fun findCompatibleView(
        viewModel: ViewModel,
        viewRegistry: Map<Class<*>, ViewInfo>,
        targetResolutionCache: ConcurrentHashMap<Class<*>, ViewInfo>
    ): ViewInfo {
        val viewModelClass = viewModel::class.java

        // Direct match in registry (fastest path)
        viewRegistry[viewModelClass]?.let {
            return it
        }

        // Check resolution cache for previously resolved indirect matches
        targetResolutionCache[viewModelClass]?.let {
            return it
        }

        // Perform full resolution chain and cache the result
        val resolved = resolveViewThroughHierarchy(viewModelClass, viewRegistry)

        // Cache the resolved view for future lookups
        resolved?.let { targetResolutionCache[viewModelClass] = it }

        return resolved ?: throw ViewNotFound(viewModel)
    }

    /**
     * Performs the full view resolution chain for types not found directly in the registry.
     * Separated from findCompatibleView to enable clean caching logic.
     */
    private fun resolveViewThroughHierarchy(
        viewModelClass: Class<*>,
        viewRegistry: Map<Class<*>, ViewInfo>,
    ): ViewInfo? {
        // Check inheritance hierarchy
        findViewInInheritanceChain(viewModelClass, viewRegistry)?.let {
            return it
        }

        // Check interfaces
        findViewInInterfaces(viewModelClass, viewRegistry)?.let {
            return it
        }

        return null
    }

    private fun findViewInInheritanceChain(
        viewModelClass: Class<*>,
        viewRegistry: Map<Class<*>, ViewInfo>,
    ): ViewInfo? {
        var currentClass: Class<*>? = viewModelClass.superclass
        while (currentClass != null && currentClass != Any::class.java) {
            viewRegistry[currentClass]?.let {
                return it
            }
            currentClass = currentClass.superclass
        }
        return null
    }

    private fun findViewInInterfaces(
        viewModelClass: Class<*>,
        viewRegistry: Map<Class<*>, ViewInfo>,
    ): ViewInfo? {
        for (interfaceClass in viewModelClass.interfaces) {
            viewRegistry[interfaceClass]?.let {
                return it
            }
        }
        return null
    }

    /**
     * Renders a view with the provided ViewModel. Handles both synchronous and asynchronous views.
     */
    private fun renderViewWithModel(
        view: Any,
        viewModel: ViewModel,
        location: String,
    ): String {
        return try {
            when (view) {
                is HtmlView<*> -> {
                    renderSynchronousView(view, viewModel)
                }

                else ->
                    throw IllegalArgumentException(
                        "Unsupported view type: ${view::class.simpleName} at $location",
                    )
            }
        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to render view at $location for model ${viewModel::class.simpleName}",
                e,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun renderSynchronousView(
        view: HtmlView<*>,
        viewModel: ViewModel,
    ): String {
        return (view as HtmlView<ViewModel>).render(viewModel)
    }
}
