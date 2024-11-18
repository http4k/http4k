package org.http4k.connect.plugin.foo

import org.http4k.connect.AutomarshalledPagedAction
import org.http4k.connect.Paged
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.format.Moshi
import org.http4k.lens.Header
import kotlin.reflect.KClass

abstract class FooPagedAction<
    ItemType : Any,
    PageType : Paged<Uri, ItemType>,
    Self : FooPagedAction<ItemType, PageType, Self>
    >(
    toResult: (List<ItemType>, Uri?) -> PageType,
    kClass: KClass<PageType>
) : AutomarshalledPagedAction<Uri, ItemType, PageType, Self>(toResult, Moshi, kClass), FooAction<PageType> {
    override fun invoke(target: Response) = Header.LINK(target)["next"]
}
