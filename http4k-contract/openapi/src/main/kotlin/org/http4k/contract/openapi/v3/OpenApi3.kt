package org.http4k.contract.openapi.v3

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoute
import org.http4k.contract.ErrorResponseRenderer
import org.http4k.contract.HttpMessageMeta
import org.http4k.contract.JsonErrorResponseRenderer
import org.http4k.contract.PathSegments
import org.http4k.contract.RouteMeta
import org.http4k.contract.Tag
import org.http4k.contract.WebCallback
import org.http4k.contract.jsonschema.JsonSchema
import org.http4k.contract.jsonschema.v2.value
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.OpenApiExtension
import org.http4k.contract.openapi.OpenApiVersion
import org.http4k.contract.openapi.OpenApiVersion._3_0_0
import org.http4k.contract.openapi.Render
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.contract.openapi.v3.BodyContent.FormContent
import org.http4k.contract.openapi.v3.BodyContent.FormContent.FormSchema
import org.http4k.contract.openapi.v3.BodyContent.NoSchema
import org.http4k.contract.openapi.v3.BodyContent.OneOfSchemaContent
import org.http4k.contract.openapi.v3.BodyContent.SchemaContent
import org.http4k.contract.openapi.v3.RequestParameter.PrimitiveParameter
import org.http4k.contract.openapi.v3.RequestParameter.SchemaParameter
import org.http4k.contract.security.Security
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.MULTIPART_FORM_DATA
import org.http4k.core.HttpMessage
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Meta
import org.http4k.lens.MultipartForm
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.lens.WebForm
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

/**
 * Contract renderer for OpenApi3 format JSON. For the JSON schema generation, naming of
 * object models will default to either reflective or hashcode based depending on if a Auto Json
 * is passed.
 */
class OpenApi3<NODE : Any>(
    private val apiInfo: ApiInfo,
    private val json: Json<NODE>,
    private val extensions: List<OpenApiExtension> = emptyList(),
    private val apiRenderer: ApiRenderer<Api<NODE>, NODE> = OpenApi3ApiRenderer(json),
    // note that this is the basic OpenApi renderer - if you want reflective Schema generation
    // then you want to use ApiRenderer.Auto() instead with a compatible JSON instance
    private val securityRenderer: SecurityRenderer = OpenApi3SecurityRenderer,
    private val errorResponseRenderer: ErrorResponseRenderer = JsonErrorResponseRenderer(json),
    private val servers: List<ApiServer> = emptyList(),
    private val version: OpenApiVersion = _3_0_0
) : ContractRenderer, ErrorResponseRenderer by errorResponseRenderer {
    private data class PathAndMethod<NODE>(val path: String, val method: Method, val pathSpec: ApiPath<NODE>)

    constructor(
        apiInfo: ApiInfo,
        json: AutoMarshallingJson<NODE>,
        extensions: List<OpenApiExtension> = emptyList(),
        servers: List<ApiServer> = emptyList(),
        version: OpenApiVersion = _3_0_0
    ) : this(apiInfo, json, extensions, ApiRenderer.Auto(json), servers = servers, version = version)

    override fun description(
        contractRoot: PathSegments,
        security: Security?,
        routes: List<ContractRoute>,
        tags: Set<Tag>,
        webhooks: Map<String, List<WebCallback>>
    ): Response {
        val allSecurities = routes.map { it.meta.security } + listOfNotNull(security)
        val paths = routes.map { it.asPath(security, contractRoot) }

        val unextended = apiRenderer.api(
            Api(
                apiInfo,
                (routes.map(ContractRoute::tags).flatten() + tags).toSet().sortedBy { it.name },
                paths
                    .groupBy { it.path }
                    .mapValues {
                        it.value.associate { pam -> pam.method.name.lowercase(Locale.getDefault()) to pam.pathSpec }
                            .toSortedMap()
                    }
                    .toSortedMap(),
                Components(
                    json.obj(paths.flatMap { it.pathSpec.definitions() }),
                    json(allSecurities.filterNotNull().combineFull())
                ),
                servers,
                webhooks.takeIf { it.isNotEmpty() }
                    ?.mapValues { (_, webhooks) ->
                        webhooks.associate {
                            it.method.name.lowercase() to it.meta.apiPath(
                                it.method,
                                it.meta.requestParams.map { it.meta })
                        }
                    },
                version.toString()
            )
        )

        return Response(OK)
            .with(json.body().toLens() of extensions.fold(unextended) { acc, next -> json(next(acc)) })
    }

    private fun ContractRoute.asPath(contractSecurity: Security?, contractRoot: PathSegments) =
        PathAndMethod(describeFor(contractRoot), method, apiPath(contractRoot, contractSecurity))

    private fun ContractRoute.apiPath(contractRoot: PathSegments, contractSecurity: Security?) =
        meta.apiPath(
            method,
            nonBodyParams,
            operationId(contractRoot),
            json(listOfNotNull(meta.security ?: contractSecurity).combineRef()),
            if (tags.isEmpty()) listOf(contractRoot.toString()) else tags.map { it.name }.toSet().sorted()
        )

    private fun RouteMeta.apiPath(
        method: Method,
        nonBodyParams: List<Meta>,
        operationId: String? = null,
        security: NODE? = null,
        tags: List<String>? = null
    ): ApiPath<NODE> {
        val body = requestBody()?.takeIf { it.required }

        return if (method in setOf(GET, HEAD) || body == null) {
            ApiPath.NoBody(
                summary,
                description,
                tags,
                nonBodyParams.map(::requestParameter),
                responses(),
                security,
                operationId,
                deprecated,
                callbacksAsApiPaths(),
            )
        } else {
            ApiPath.WithBody(
                summary,
                description,
                tags,
                nonBodyParams.map(::requestParameter),
                body,
                responses(),
                security,
                operationId,
                deprecated,
                callbacksAsApiPaths()
            )
        }
    }

    private fun RouteMeta.callbacksAsApiPaths() =
        callbacks?.mapValues { (_, callbackRoutes) ->
            callbackRoutes.mapValues { (_, rcb) ->
                mapOf(
                    rcb.method.name.lowercase() to rcb.meta.apiPath(rcb.method, rcb.meta.requestParams.map { it.meta })
                )
            }
        }

    private fun RouteMeta.responses() = responses
        .groupBy { it.message.status.code.toString() }
        .map {
            it.key to
                ResponseContents<NODE>(
                    it.value
                        .map { it.description }.toSortedSet().joinToString(","), it.value.collectSchemas()
                )
        }.toMap()

    private fun List<HttpMessageMeta<Response>>.collectSchemas() = groupBy { CONTENT_TYPE(it.message) }
        .mapValues {
            when (it.value.size) {
                1 -> it.value.first().toSchemaContent()
                else -> OneOfSchemaContent<NODE>(it.value.map { it.toSchemaContent() })
            }
        }
        .mapNotNull { i -> i.key?.let { it.value to i.value } }
        .toMap()

    private fun requestParameter(meta: Meta): RequestParameter<NODE> =
        when (val paramMeta: ParamMeta = meta.paramMeta) {
            ObjectParam -> SchemaParameter(
                meta, "{}".toSchema()
                    .appendFields(meta.schemaFields())
            )

            is ArrayParam -> PrimitiveParameter(meta, json {
                val itemType = paramMeta.itemType()
                obj(
                    listOf(
                        "type" to string("array"),
                        "items" to when (itemType) {
                            is EnumParam<*> -> apiRenderer.toSchema(
                                itemType.clz.java.enumConstants[0],
                                meta.name,
                                null
                            ).definitions.first().second

                            else -> obj("type" to string(itemType.value))
                        }
                    )
                        + meta.schemaFields()
                )
            })

            is EnumParam<*> -> SchemaParameter(
                meta,
                json {
                    apiRenderer.toSchema(
                        paramMeta.clz.java.enumConstants[0],
                        meta.name,
                        null
                    ).appendFields(
                        meta.schemaFields()
                    )
                }
            )

            else -> PrimitiveParameter(meta, json {
                obj(listOf("type" to string(paramMeta.value)) + meta.schemaFields())
            })
        }

    private fun Meta.schemaFields(): List<Pair<String, NODE>> =
        schemaMetadata()?.map { entry -> entry.key to asNode(entry.value) } ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun Meta.schemaMetadata() = metadata.let { it["schema"] as? Map<String, Any> }

    private fun JsonSchema<NODE>.appendFields(fields: List<Pair<String, NODE>>): JsonSchema<NODE> =
        JsonSchema(
            this@OpenApi3.json { obj(fields(node) + fields) },
            definitions
        )

    private fun asNode(thing: Any?): NODE =
        this@OpenApi3.json {
            when (thing) {
                is Boolean -> boolean(thing)
                is Int -> number(thing)
                is Double -> number(thing)
                is BigDecimal -> number(thing)
                is BigInteger -> number(thing)
                is Long -> number(thing)
                is Iterable<*> -> array(thing.map { asNode(it) })
                is Map<*, *> -> obj(thing.map { it.key.toString() to asNode(it.value) }.toList())
                else -> string(thing.toString())
            }
        }

    private fun RouteMeta.requestBody(): RequestContents<NODE>? {
        val noSchema = consumes.map { it.value to NoSchema(json { obj("type" to string(StringParam.value)) }) }

        val withSchema = requests.mapNotNull { req ->
            with(CONTENT_TYPE(req.message)) {
                when (this?.withNoDirectives()) {
                    APPLICATION_FORM_URLENCODED.withNoDirectives() -> value to
                        (body?.metas?.let {
                            FormContent(FormSchema(
                                it.associateWith {
                                    (req.example as? WebForm)?.let { form -> form.fields[it.name] }
                                }
                            ))
                        } ?: SchemaContent("".toSchema(), null))

                    MULTIPART_FORM_DATA.withNoDirectives() -> value to
                        (body?.metas?.let {
                            FormContent(FormSchema(
                                it.associateWith {
                                    (req.example as? MultipartForm)
                                        ?.let { form -> form.fields[it.name]?.map { it.value } }
                                }
                            ))
                        } ?: SchemaContent("".toSchema(), null))

                    null -> null
                    else -> value to req.toSchemaContent()
                }
            }
        }

        val collectedWithSchema = withSchema
            .groupBy { it.first }
            .mapValues {
                when (it.value.size) {
                    1 -> it.value.first().second
                    else -> OneOfSchemaContent<NODE>(it.value.map { it.second })
                }
            }.toList()

        return (noSchema + collectedWithSchema)
            .takeIf { it.isNotEmpty() }
            ?.let { RequestContents(it.toMap()) }
    }

    private fun HttpMessageMeta<HttpMessage>.toSchemaContent(): BodyContent {
        fun exampleSchemaIsValid(schema: JsonSchema<NODE>) =
            when (example) {
                is Array<*>, is Iterable<*> -> !json.fields(schema.node).toMap().containsKey("\$ref")
                else -> apiRenderer.toSchema(object {}, refModelNamePrefix = schemaPrefix) != schema
            }

        val jsonSchema = example
            ?.let { apiRenderer.toSchema(it, definitionId, schemaPrefix) }
            ?.takeIf(::exampleSchemaIsValid)
            ?: message.bodyString().toSchema(definitionId)

        return SchemaContent(jsonSchema, message.bodyString().safeParse())
    }

    private fun String.toSchema(definitionId: String? = null) = safeParse()
        ?.let { apiRenderer.toSchema(it, definitionId, null) }
        ?: JsonSchema(json.obj(), emptySet())

    private fun List<Security>.combineFull(): Render<NODE> = {
        obj(mapNotNull { securityRenderer.full<NODE>(it) }.flatMap { fields(this(it)) })
    }

    private fun List<Security>.combineRef(): Render<NODE> = {
        array(mapNotNull { securityRenderer.ref<NODE>(it) }.flatMap {
            this(it).let { if (typeOf(it) == JsonType.Array) elements(it) else listOf(it) }
        })
    }

    private fun String.safeParse(): NODE? = try {
        json.parse(this)
    } catch (e: Exception) {
        null
    }

    companion object
}
