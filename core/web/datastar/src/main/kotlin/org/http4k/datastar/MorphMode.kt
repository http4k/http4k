package org.http4k.datastar

enum class MorphMode {

    // Morphs the outer HTML of the elements. This is the default (and recommended) mode.
    outer,

    // Morphs the inner HTML of the elements.
    inner,

    // Replaces the outer HTML of the elements.
    replace,

    // Prepends the fragment to the target’s children.
    prepend,

    // Appends the fragment to the target’s children.
    append,

    // Inserts the fragment before the target as a sibling.
    before,

    // Inserts the fragment after the target as a sibling.
    after,

    // Removes the target elements from DOM.
    remove
}
