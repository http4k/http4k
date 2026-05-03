/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.storage

import org.http4k.ai.a2a.model.PageToken
import org.http4k.ai.a2a.model.PushNotificationConfigPage
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.model.Tenant
import java.util.concurrent.ConcurrentHashMap

interface PushNotificationConfigStorage {
    fun store(config: TaskPushNotificationConfig)
    fun get(id: PushNotificationConfigId, tenant: Tenant? = null): TaskPushNotificationConfig?
    fun delete(id: PushNotificationConfigId, tenant: Tenant? = null)
    fun list(taskId: TaskId, pageSize: Int? = null, pageToken: PageToken? = null, tenant: Tenant? = null): PushNotificationConfigPage

    companion object {
        fun InMemory() = object : PushNotificationConfigStorage {
            private val NO_TENANT = Tenant.of("__no_tenant__")
            private val tenants = ConcurrentHashMap<Tenant, ConcurrentHashMap<PushNotificationConfigId, TaskPushNotificationConfig>>()

            private fun configsFor(tenant: Tenant?) = tenants.getOrPut(tenant ?: NO_TENANT) { ConcurrentHashMap() }

            override fun store(config: TaskPushNotificationConfig) {
                configsFor(config.tenant)[config.id] = config
            }

            override fun get(id: PushNotificationConfigId, tenant: Tenant?) = configsFor(tenant)[id]

            override fun delete(id: PushNotificationConfigId, tenant: Tenant?) {
                configsFor(tenant).remove(id)
            }

            override fun list(taskId: TaskId, pageSize: Int?, pageToken: PageToken?, tenant: Tenant?): PushNotificationConfigPage {
                val all = configsFor(tenant).values.filter { it.taskId == taskId }.sortedBy { it.id.value }
                return if (pageSize != null) {
                    val startIndex = pageToken?.value?.toIntOrNull() ?: 0
                    val endIndex = minOf(startIndex + pageSize, all.size)
                    PushNotificationConfigPage(all.subList(startIndex, endIndex), if (endIndex < all.size) PageToken.of(endIndex.toString()) else null)
                } else {
                    PushNotificationConfigPage(all)
                }
            }
        }
    }
}
