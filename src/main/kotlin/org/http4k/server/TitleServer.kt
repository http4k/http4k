package org.http4k.server

import org.http4k.routing.routes
import org.http4k.server.endpoints.EchoMessage
import org.http4k.server.endpoints.GetBasepathNometa
import org.http4k.server.endpoints.GetBasepathProduces_and_consumes
import org.http4k.server.endpoints.PostBasepathAnd_auth
import org.http4k.server.endpoints.PostBasepathBasic_auth
import org.http4k.server.endpoints.PostBasepathBearer_auth
import org.http4k.server.endpoints.PostBasepathBody_auto_schema
import org.http4k.server.endpoints.PostBasepathBody_form
import org.http4k.server.endpoints.PostBasepathBody_json_list_schema
import org.http4k.server.endpoints.PostBasepathBody_json_response
import org.http4k.server.endpoints.PostBasepathBody_json_schema
import org.http4k.server.endpoints.PostBasepathBody_string
import org.http4k.server.endpoints.PostBasepathCookies
import org.http4k.server.endpoints.PostBasepathHeaders
import org.http4k.server.endpoints.PostBasepathOauth2_auth
import org.http4k.server.endpoints.PostBasepathOr_auth
import org.http4k.server.endpoints.PostBasepathPaths_firstName_Bertrand_age
import org.http4k.server.endpoints.PostBasepathQueries
import org.http4k.server.endpoints.PostBasepathReturning
import org.http4k.server.endpoints.PutBasepathBody_auto_map
import org.http4k.server.endpoints.PutBasepathMultipart_fields

object TitleServer {
	operator fun invoke() = routes(
		PostBasepathAnd_auth()
	, 	PostBasepathBasic_auth()
	, 	PostBasepathBearer_auth()
	, 	PutBasepathBody_auto_map()
	, 	PostBasepathBody_auto_schema()
	, 	PostBasepathBody_form()
	, 	PostBasepathBody_json_list_schema()
	, 	PostBasepathBody_json_response()
	, 	PostBasepathBody_json_schema()
	, 	PostBasepathBody_string()
	, 	PostBasepathCookies()
	, 	EchoMessage()
	, 	PostBasepathHeaders()
	, 	PutBasepathMultipart_fields()
	, 	GetBasepathNometa()
	, 	PostBasepathOauth2_auth()
	, 	PostBasepathOr_auth()
	, 	PostBasepathPaths_firstName_Bertrand_age()
	, 	GetBasepathProduces_and_consumes()
	, 	PostBasepathQueries()
	, 	PostBasepathReturning()
	)
}

fun main() {
	TitleServer().asServer(SunHttp(8000)).start()
}
