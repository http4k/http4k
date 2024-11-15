package org.http4k.connect.github.api

import org.http4k.connect.AutomarshalledPagedAction
import org.http4k.connect.Paged
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.format.AutoMarshalling
import org.http4k.lens.Header
import kotlin.reflect.KClass

abstract class PagedGitHubAction<
    ItemType : Any,
    PageType : Paged<Uri, ItemType>,
    Self : PagedGitHubAction<ItemType, PageType, Self>
    >(
    toResult: (List<ItemType>, Uri?) -> PageType,
    autoMarshalling: AutoMarshalling,
    kClass: KClass<PageType>
) : AutomarshalledPagedAction<Uri, ItemType, PageType, Self>(toResult, autoMarshalling, kClass),
    GitHubAction<PageType> {
    override fun invoke(target: Response) = Header.LINK(target)["next"]
}
