/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.hotreload

import java.net.URL
import java.net.URLClassLoader

class HotReloadClassLoader(urls: Array<URL>) : URLClassLoader(urls) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            var clazz = findLoadedClass(name)
            if (clazz == null) {
                clazz = try {
                    findClass(name)
                } catch (e: ClassNotFoundException) {
                    super.loadClass(name, false)
                }
            }
            if (resolve) resolveClass(clazz)
            return clazz
        }
    }
}
