package org.http4k.connect.openai

import org.http4k.connect.openai.auth.PluginAuth
import org.http4k.connect.openai.auth.noauth.NoAuth
import org.http4k.connect.openai.endpoints.GetManifest
import org.http4k.connect.openai.endpoints.ProtectPluginRoutes
import org.http4k.connect.openai.endpoints.ServeLogo
import org.http4k.connect.openai.model.Api
import org.http4k.connect.openai.model.Email
import org.http4k.connect.openai.model.Manifest
import org.http4k.contract.ContractRoute
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.ApiServer
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.Uri
import org.http4k.routing.routes

fun info(
    apiVersion: String,
    pluginUrl: Uri,
    contactEmail: Email,
    humanDescription: Pair<String, String>,
    modelDescription: Pair<String, String> = humanDescription,
    logoUrl: Uri = pluginUrl.path("logo.png"),
    legalInfoUrl: Uri = pluginUrl.path("legal")
) = PluginInfo(apiVersion, pluginUrl, contactEmail, humanDescription, modelDescription, logoUrl, legalInfoUrl)

@ConsistentCopyVisibility
data class PluginInfo internal constructor(
    val apiVersion: String,
    val pluginUrl: Uri,
    val contactEmail: Email,
    val humanDescription: Pair<String, String>,
    val modelDescription: Pair<String, String> = humanDescription,
    val logoUrl: Uri = pluginUrl.path("logo.png"),
    val legalInfoUrl: Uri = pluginUrl.path("legal")
)

fun openAiPlugin(
    info: PluginInfo,
    auth: PluginAuth = NoAuth,
    vararg routes: ContractRoute
) = routes(
    *(
        listOf(
            GetManifest(with(info) {
                Manifest(
                    humanDescription.first,
                    humanDescription.second,
                    modelDescription.first,
                    modelDescription.second,
                    Api(info.pluginUrl.path("/openapi.json"), auth != NoAuth),
                    auth.manifestDescription,
                    info.contactEmail,
                    info.logoUrl,
                    info.legalInfoUrl
                )
            }),
            contract {
                renderer =
                    OpenApi3(
                        ApiInfo(
                            info.modelDescription.first,
                            info.apiVersion,
                            info.modelDescription.first
                        ),
                        servers = listOf(ApiServer(info.pluginUrl))
                    )
                descriptionPath = "/openapi.json"
                postSecurityFilter = ProtectPluginRoutes(auth)
                this.routes += routes.toList()
            },
            ServeLogo(),
        ) + auth.authRoutes
        ).toTypedArray()
)
