package org.http4k

internal object Http4kLicenseCheck {
    @Volatile
    private var warningDisplayed = false
    private val lock = Object()

    init {
        if (!warningDisplayed) {
            synchronized(lock) {
                if (!warningDisplayed) {
                    System.err.println(
                        """
                        |╔══════════════════════════════════════════════════════════════════════════════════════╗
                        |║                                                                                      ║
                        |║ # Important License Notice                                                           ║
                        |║                                                                                      ║
                        |║ This version of http4k v5.X.X.X is an LTS licensed version under the http4k          ║
                        |║ Commercial License. The version you are using requires a valid commercial            ║
                        |║ subscription to be legally used in your project.                                     ║
                        |║                                                                                      ║
                        |║ ## Your Options                                                                      ║
                        |║                                                                                      ║
                        |║ 1. **Obtain a Commercial License with the http4k Enterprise Edition subscription**   ║
                        |║    - Visit https://http4k.org/commercial-license for license details                 ║
                        |║    - See https://http4k.org/enterprise for details of how to obtain a propertly      ║
                        |║      licensed version of http4k Enterprise Edition                                   ║
                        |║                                                                                      ║
                        |║ 2. **Use the Open Source Version**                                                   ║
                        |║    - Downgrade to http4k version 5.47.0.0 or earlier                                 ║
                        |║    - These versions remain available under the Apache 2.0 license                    ║
                        |║                                                                                      ║
                        |║ ## Need Help?                                                                        ║
                        |║ - For commercial licensing queries: sales@http4k.org                                 ║
                        |║ ---                                                                                  ║
                        |║                                                                                      ║
                        |║ This notice appears because you are using http4k Version 5.48.0.0 or above without   ║
                        |║ a valid commercial subscription. Please take action to ensure your usage complies    ║
                        |║ with the appropriate licensing terms.                                                ║
                        |╚══════════════════════════════════════════════════════════════════════════════════════╝
                    """.trimMargin()
                    )
                    warningDisplayed = true
                }
            }
        }
    }
}
