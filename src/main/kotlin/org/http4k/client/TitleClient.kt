package org.http4k.client

import kotlin.Boolean
import kotlin.Int
import kotlin.String
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.auto
import org.http4k.lens.Cookies
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.model.Object1089648089
import org.http4k.model.Object2055192556
import org.http4k.model.PostBasepathBodyformXwwwformurlencodedRequest
import org.http4k.model.PutBasepathBodyautomapJsonRequest
import org.http4k.model.PutBasepathMultipartfieldsFormdataRequest
import org.http4k.model.SomeDefinitionId
import org.http4k.model.SomeOtherId

class TitleClient(
	private val httpHandler: HttpHandler
) {
	fun postBasepathAnd_auth() {

		val httpReq = Request(Method.POST, "/basepath/and_auth")

		httpHandler(httpReq)
	}

	fun postBasepathBasic_auth() {

		val httpReq = Request(Method.POST, "/basepath/basic_auth")

		httpHandler(httpReq)
	}

	fun postBasepathBearer_auth() {

		val httpReq = Request(Method.POST, "/basepath/bearer_auth")

		httpHandler(httpReq)
	}

	fun putBasepathBody_auto_map(request: PutBasepathBodyautomapJsonRequest) {

		val putBasepathBodyautomapJsonRequestLens =
				Body.auto<PutBasepathBodyautomapJsonRequest>().toLens()
		val httpReq = Request(Method.PUT, "/basepath/body_auto_map")
			.with(putBasepathBodyautomapJsonRequestLens of request)

		httpHandler(httpReq)
	}

	fun postBasepathBody_auto_schema(request: SomeOtherId) {

		val someOtherIdLens = Body.auto<SomeOtherId>().toLens()
		val httpReq = Request(Method.POST, "/basepath/body_auto_schema")
			.with(someOtherIdLens of request)

		httpHandler(httpReq)
	}

	fun postBasepathBody_form(request: PostBasepathBodyformXwwwformurlencodedRequest) {

		val postBasepathBodyformXwwwformurlencodedRequestLens =
				Body.auto<PostBasepathBodyformXwwwformurlencodedRequest>().toLens()
		val httpReq = Request(Method.POST, "/basepath/body_form")
			.with(postBasepathBodyformXwwwformurlencodedRequestLens of request)

		httpHandler(httpReq)
	}

	fun postBasepathBody_json_list_schema(request: Object1089648089) {

		val object1089648089Lens = Body.auto<Object1089648089>().toLens()
		val httpReq = Request(Method.POST, "/basepath/body_json_list_schema")
			.with(object1089648089Lens of request)

		httpHandler(httpReq)
	}

	fun postBasepathBody_json_response(): Object1089648089 {

		val object1089648089Lens = Body.auto<Object1089648089>().toLens()
		val httpReq = Request(Method.POST, "/basepath/body_json_response")
		return object1089648089Lens(httpHandler(httpReq))
	}

	fun postBasepathBody_json_schema(request: SomeDefinitionId) {

		val someDefinitionIdLens = Body.auto<SomeDefinitionId>().toLens()
		val httpReq = Request(Method.POST, "/basepath/body_json_schema")
			.with(someDefinitionIdLens of request)

		httpHandler(httpReq)
	}

	fun postBasepathBody_string(request: String) {

		val postBasepathBodystringPlainRequestLens = Body.auto<String>().toLens()
		val httpReq = Request(Method.POST, "/basepath/body_string")
			.with(postBasepathBodystringPlainRequestLens of request)

		httpHandler(httpReq)
	}

	fun postBasepathCookies(b: String, s: String?) {

		val bLens = Cookies.required("b")
		val sLens = Cookies.optional("s")
		val httpReq = Request(Method.POST, "/basepath/cookies")
			.with(bLens of Cookie("b", b))
			.with(sLens of Cookie("s", s ?: ""))

		httpHandler(httpReq)
	}

	fun echoMessage() {

		val httpReq = Request(Method.GET, "/basepath/descriptions")

		httpHandler(httpReq)
	}

	fun postBasepathHeaders(
		b: Boolean,
		s: String?,
		i: Int?,
		j: String?
	) {

		val bLens = Header.boolean().required("b")
		val sLens = Header.string().optional("s")
		val iLens = Header.int().optional("i")
		val jLens = Header.string().optional("j")
		val httpReq = Request(Method.POST, "/basepath/headers")
			.with(bLens of b)
			.with(sLens of s)
			.with(iLens of i)
			.with(jLens of j)

		httpHandler(httpReq)
	}

	fun putBasepathMultipart_fields(request: PutBasepathMultipartfieldsFormdataRequest) {

		val putBasepathMultipartfieldsFormdataRequestLens =
				Body.auto<PutBasepathMultipartfieldsFormdataRequest>().toLens()
		val httpReq = Request(Method.PUT, "/basepath/multipart_fields")
			.with(putBasepathMultipartfieldsFormdataRequestLens of request)

		httpHandler(httpReq)
	}

	fun getBasepathNometa() {

		val httpReq = Request(Method.GET, "/basepath/nometa")

		httpHandler(httpReq)
	}

	fun postBasepathOauth2_auth() {

		val httpReq = Request(Method.POST, "/basepath/oauth2_auth")

		httpHandler(httpReq)
	}

	fun postBasepathOr_auth() {

		val httpReq = Request(Method.POST, "/basepath/or_auth")

		httpHandler(httpReq)
	}

	fun postBasepathPaths_firstName_Bertrand_age(firstName: String, age: Boolean) {

		val firstNameLens = Path.string().of("firstName")
		val ageLens = Path.boolean().of("age")
		val httpReq = Request(Method.POST, "/basepath/paths/${firstName}/bertrand/${age}")

		httpHandler(httpReq)
	}

	fun getBasepathProduces_and_consumes() {

		val httpReq = Request(Method.GET, "/basepath/produces_and_consumes")

		httpHandler(httpReq)
	}

	fun postBasepathQueries(
		b: Boolean,
		s: String?,
		i: Int?,
		j: String?
	) {

		val bLens = Query.boolean().required("b")
		val sLens = Query.string().optional("s")
		val iLens = Query.int().optional("i")
		val jLens = Query.string().optional("j")
		val httpReq = Request(Method.POST, "/basepath/queries")
			.with(bLens of b)
			.with(sLens of s)
			.with(iLens of i)
			.with(jLens of j)

		httpHandler(httpReq)
	}

	fun postBasepathReturning(): Object2055192556 {

		val object2055192556Lens = Body.auto<Object2055192556>().toLens()
		val httpReq = Request(Method.POST, "/basepath/returning")
		return object2055192556Lens(httpHandler(httpReq))
	}
}
