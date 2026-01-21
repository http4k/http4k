package org.http4k.template

import AtRootBobRocker

class RockerTemplatesTest : TemplatesContract<RockerTemplates>(RockerTemplates()) {
    override fun onClasspathViewModel(items: List<Item>) = OnClasspathRocker().items(items)

    override fun atRootViewModel(items: List<Item>) = AtRootBobRocker().items(items)

    override fun onClasspathNotAtRootViewModel(items: List<Item>) = OnClasspathNotAtRootRocker().items(items)
    
    override fun checkCauseOfNonExistent(exception: ViewNotFound) {
        // Rocker does not translate exceptions when a template cannot be found
    }
}
