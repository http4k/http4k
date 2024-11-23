package org.http4k.datastar

enum class MergeMode {
    // Merges the fragment using Idiomorph. This is the default merge strategy.
    morph,

    // Replaces the target’s innerHTML with the fragment.
    inner,

    // Replaces the target’s outerHTML with the fragment.
    outer,

    // Prepends the fragment to the target’s children.
    prepend,

    // Appends the fragment to the target’s children.
    append,

    // Inserts the fragment before the target as a sibling.
    before,

    // Inserts the fragment after the target as a sibling.
    after,

    // Merges attributes from the fragment into the target – useful for updating a store.
    upsertAttributes
}
